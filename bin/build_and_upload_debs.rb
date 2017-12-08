#!/usr/bin/env ruby

PROJECT_ROOT = File.expand_path(File.dirname(__FILE__) + '/..')

require 'yaml'

if __FILE__ == $0
  service_map = YAML::load_file("#{PROJECT_ROOT}/bin/services.yaml")
  system "#{PROJECT_ROOT}/bin/build.rb"
  exit($?.exitstatus)
end
