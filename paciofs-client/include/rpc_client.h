/*
 * Copyright (c) 2019, Zuse Institute Berlin.
 *
 * Licensed under the New BSD License, see LICENSE file for details.
 *
 */

#ifndef RPC_CLIENT_H
#define RPC_CLIENT_H

#include <grpcpp/grpcpp.h>
#include <string>

namespace paciofs {
namespace grpc {

template <typename Service>
class RpcClient {
 public:
  // uses TLS if given paths, insecure credentials otherwise
  explicit RpcClient(std::string const& target, std::string const& cert_chain,
                     std::string const& private_key,
                     std::string const& root_certs);

 private:
  std::string ReadPem(std::string const& path);

  void CreateMetadata();

  // sent in each request as metadata
  std::string metadata_user_;
  std::string metadata_group_;

  std::unique_ptr<typename Service::Stub> stub_;

 protected:
  std::unique_ptr<typename Service::Stub> const& Stub();

  void SetMetadata(::grpc::ClientContext& context);
};

}  // namespace grpc
}  // namespace paciofs

#endif  // RPC_CLIENT_H
