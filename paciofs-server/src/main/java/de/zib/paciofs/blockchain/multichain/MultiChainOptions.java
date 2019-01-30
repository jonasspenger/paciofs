/*
 * Copyright (c) 2019, Zuse Institute Berlin.
 *
 * Licensed under the New BSD License, see LICENSE file for details.
 *
 */

package de.zib.paciofs.blockchain.multichain;

public class MultiChainOptions {
  public static final String BACKOFF_MILLISECONDS = "multichain.multichaind.backoff.milliseconds";
  public static final String BACKOFF_RETRIES = "multichain.multichaind.backoff.retries";

  public static final String BLOCKCHAIN_NAME_KEY = "multichain.blockchain-name";

  public static final String DAEMON_OPTIONS_KEY = "multichain.multichaind.options";

  public static final String PATH_KEY = "multichain.path";

  public static final String PROTOCOL_VERSION_KEY = "multichain.protocol-version";

  public static final String PORT_KEY = "multichain.multichaind.options.port";

  public static final String RPC_CONNECT_KEY = "rpcconnect";
  public static final String RPC_PORT_KEY = "rpcport";
  public static final String RPC_USER_KEY = "rpcuser";
  public static final String RPC_PASSWORD_KEY = "rpcpassword";

  public static final String UTIL_OPTIONS_KEY = "multichain.multichain-util.options";

  private MultiChainOptions() {}
}