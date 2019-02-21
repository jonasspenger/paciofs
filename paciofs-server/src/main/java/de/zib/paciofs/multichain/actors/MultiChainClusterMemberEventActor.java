/*
 * Copyright (c) 2019, Zuse Institute Berlin.
 *
 * Licensed under the New BSD License, see LICENSE file for details.
 *
 */

package de.zib.paciofs.multichain.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.japi.pf.ReceiveBuilder;
import de.zib.paciofs.logging.Markers;
import de.zib.paciofs.multichain.rpc.MultiChainRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

public class MultiChainClusterMemberEventActor extends AbstractActor {
  private enum NodeCommand {
    ADD("add"),
    REMOVE("remove");

    private final String verb;

    NodeCommand(String verb) {
      this.verb = verb;
    }
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(MultiChainClusterMemberEventActor.class);

  private final Cluster cluster;

  private final MultiChainRpcClient client;

  private MultiChainClusterMemberEventActor(MultiChainRpcClient client) {
    this.cluster = Cluster.get(this.context().system());
    this.client = client;
  }

  public static Props props(MultiChainRpcClient client) {
    return Props.create(MultiChainClusterMemberEventActor.class,
        () -> new MultiChainClusterMemberEventActor(client));
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();
    this.cluster.subscribe(
        this.self(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class);
  }

  @Override
  public void postStop() throws Exception {
    this.cluster.unsubscribe(this.self());
    super.postStop();
  }

  @Override
  public Receive createReceive() {
    final ReceiveBuilder builder = this.receiveBuilder();

    builder.match(ClusterEvent.MemberDowned.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.REMOVE);
    });
    builder.match(ClusterEvent.MemberExited.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.REMOVE);
    });
    builder.match(ClusterEvent.MemberJoined.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.ADD);
    });
    builder.match(ClusterEvent.MemberLeft.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.REMOVE);
    });
    builder.match(ClusterEvent.MemberRemoved.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.REMOVE);
    });
    builder.match(ClusterEvent.MemberUp.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.ADD);
    });
    builder.match(ClusterEvent.MemberWeaklyUp.class, event -> {
      MultiChainClusterMemberEventActor.this.addRemoveNode(event.member(), NodeCommand.ADD);
    });

    return builder.build();
  }

  private void addRemoveNode(Member member, NodeCommand command) {
    final Option<String> host = member.address().host();
    if (host.isEmpty()) {
      LOG.warn("Cannot {} node with empty host: {}", command.verb, member);
    } else {
      try {
        LOG.trace("Node {}: {}", command.verb, host.get());
        this.client.addNode(host.get(), command.verb);
        LOG.trace("Node {} successful: {}", command.verb, host.get());
      } catch (GenericRpcException e) {
        LOG.warn("Node {} failed: {}", command.verb, e.getMessage());
        LOG.warn(Markers.EXCEPTION, "Node " + command.verb + " failed", e);
      }
    }

    // have MultiChain measure latency and backlog
    this.client.ping();
  }
}