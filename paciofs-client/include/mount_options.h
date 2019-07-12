/*
 * Copyright (c) 2019, Zuse Institute Berlin.
 *
 * Licensed under the New BSD License, see LICENSE file for details.
 *
 */

#ifndef MOUNT_OPTIONS_H
#define MOUNT_OPTIONS_H

#include "options.h"

#include <string>
#include <vector>

namespace paciofs {
namespace mount {
namespace options {

class MountOptions : public paciofs::options::Options {
 public:
  MountOptions();

  ~MountOptions();

  bool AsyncWrites() const;

  std::vector<std::string> const& FuseOptions() const;

  std::string const& MountPoint() const;

  std::string const& VolumeName() const;

 private:
  bool async_writes_;
  std::vector<std::string> fuse_options_;
  std::string mount_point_;
  std::string volume_name_;
};

}  // namespace options
}  // namespace mount
}  // namespace paciofs

#endif  // MOUNT_OPTIONS_H
