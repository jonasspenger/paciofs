/*
 * Copyright (c) 2019, Zuse Institute Berlin.
 *
 * Licensed under the New BSD License, see LICENSE file for details.
 *
 */

package de.zib.paciofs.io.posix.grpc;

import akka.grpc.javadsl.Metadata;
import com.google.protobuf.ByteString;
import de.zib.paciofs.grpc.PacioFsGrpcUtil;
import de.zib.paciofs.grpc.messages.Ping;
import de.zib.paciofs.io.posix.grpc.messages.Dir;
import de.zib.paciofs.io.posix.grpc.messages.Errno;
import de.zib.paciofs.io.posix.grpc.messages.Stat;
import de.zib.paciofs.logging.Markers;
import de.zib.paciofs.multichain.abstractions.MultiChainFileSystem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PosixIoServiceImpl implements PosixIoServicePowerApi {
  private static final Logger LOG = LoggerFactory.getLogger(PosixIoServiceImpl.class);

  private final MultiChainFileSystem multiChainFileSystem;

  public PosixIoServiceImpl(MultiChainFileSystem fileSystem) {
    this.multiChainFileSystem = fileSystem;
  }

  @Override
  public CompletionStage<PingResponse> ping(PingRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "ping({})", in);

    final Ping ping = Ping.newBuilder().build();
    final PingResponse out = PingResponse.newBuilder().setPing(ping).build();

    PacioFsGrpcUtil.traceMessages(LOG, "ping({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<StatResponse> stat(StatRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "stat({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final StatResponse.Builder builder = StatResponse.newBuilder();
    try {
      // TODO check for missing metadata
      final int user = Integer.parseInt(metadata.getText("x-user").get());
      final int group = Integer.parseInt(metadata.getText("x-group").get());

      final Stat stat = this.multiChainFileSystem.stat(in.getPath(), user, group);
      builder.setStat(stat);
    } catch (FileNotFoundException e) {
      LOG.warn(Markers.EXCEPTION, "Could not stat file {}", in.getPath(), e);
      error = Errno.ERRNO_ENOENT;
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not stat file {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final StatResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "stat({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<MkNodResponse> mkNod(MkNodRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "mkNod({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final MkNodResponse.Builder builder = MkNodResponse.newBuilder();

    try {
      if (!this.multiChainFileSystem.mkNod(in.getPath(), in.getMode(), in.getDev())) {
        LOG.warn("Could not create node {}", in.getPath());
        // TODO this is not accurate, find out proper reason
        error = Errno.ERRNO_EIO;
      }
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not create node {}", in.getPath(), e);
      // TODO this is not accurate, find out proper reason
      error = Errno.ERRNO_EIO;
    }

    final MkNodResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "mkNod({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<MkDirResponse> mkDir(MkDirRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "mkDir({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final MkDirResponse.Builder builder = MkDirResponse.newBuilder();
    if (!this.multiChainFileSystem.mkDir(in.getPath(), in.getMode())) {
      LOG.warn("Could not create directory {}", in.getPath());
      // TODO this is not accurate, find out proper reason
      error = Errno.ERRNO_EIO;
    }

    final MkDirResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "mkDir({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<ChModResponse> chMod(ChModRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "chMod({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final ChModResponse.Builder builder = ChModResponse.newBuilder();
    if (!this.multiChainFileSystem.chMod(in.getPath(), in.getMode())) {
      LOG.warn("Could not change file mode for {}", in.getPath());
      error = Errno.ERRNO_EIO;
    }

    final ChModResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "chMod({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<ChOwnResponse> chOwn(ChOwnRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "chOwn({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final ChOwnResponse.Builder builder = ChOwnResponse.newBuilder();
    if (!this.multiChainFileSystem.chOwn(in.getPath(), in.getUid(), in.getGid())) {
      LOG.warn("Could not change file owner for {}", in.getPath());
      error = Errno.ERRNO_EIO;
    }

    final ChOwnResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "chOwn({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<OpenResponse> open(OpenRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "open({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final OpenResponse.Builder builder = OpenResponse.newBuilder();
    try {
      final long fh = this.multiChainFileSystem.open(in.getPath(), in.getFlags());
      builder.setFh(fh);
    } catch (FileNotFoundException e) {
      LOG.warn(Markers.EXCEPTION, "Could not open file {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final OpenResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "open({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<ReadResponse> read(ReadRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "read({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final ReadResponse.Builder builder = ReadResponse.newBuilder();
    try {
      final ByteBuffer destination = ByteBuffer.allocateDirect(in.getSize());
      final int n =
          this.multiChainFileSystem.read(in.getPath(), destination, in.getOffset(), in.getFh());
      builder.setBuf(ByteString.copyFrom(destination));
      builder.setN(n);
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not read file {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final ReadResponse out = builder.setError(error).build();

    // TODO do not trace the entire output
    PacioFsGrpcUtil.traceMessages(LOG, "read({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<WriteResponse> write(WriteRequest in, Metadata metadata) {
    // TODO do not trace the entire input
    PacioFsGrpcUtil.traceMessages(LOG, "write({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final WriteResponse.Builder builder = WriteResponse.newBuilder();
    try {
      final int n = this.multiChainFileSystem.write(
          in.getPath(), in.getBuf().asReadOnlyByteBuffer(), in.getOffset(), in.getFh());
      builder.setN(n);
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not write file {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final WriteResponse out = builder.setError(error).build();

    // TODO do not trace the entire input/output
    PacioFsGrpcUtil.traceMessages(LOG, "write({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<ReadDirResponse> readDir(ReadDirRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "readDir({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final ReadDirResponse.Builder builder = ReadDirResponse.newBuilder();
    try {
      final List<Dir> entries = this.multiChainFileSystem.readDir(in.getPath());
      builder.addAllDirs(entries);
    } catch (FileNotFoundException e) {
      LOG.warn(Markers.EXCEPTION, "Could not read directory {}", in.getPath(), e);
      error = Errno.ERRNO_ENOENT;
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not read directory {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final ReadDirResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "readDir({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }

  @Override
  public CompletionStage<CreateResponse> create(CreateRequest in, Metadata metadata) {
    PacioFsGrpcUtil.traceMessages(LOG, "create({})", in);

    Errno error = Errno.ERRNO_ESUCCESS;
    final CreateResponse.Builder builder = CreateResponse.newBuilder();
    try {
      final long fh = this.multiChainFileSystem.create(in.getPath(), in.getMode(), in.getFlags());
      builder.setFh(fh);
    } catch (IOException e) {
      LOG.warn(Markers.EXCEPTION, "Could not create file {}", in.getPath(), e);
      error = Errno.ERRNO_EIO;
    }

    final CreateResponse out = builder.setError(error).build();

    PacioFsGrpcUtil.traceMessages(LOG, "create({}): {}", in, out);
    return CompletableFuture.completedFuture(out);
  }
}
