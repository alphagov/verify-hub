#!/usr/bin/env ruby

PROJECT_ROOT = File.expand_path(File.dirname(__FILE__) + '/..')

require 'fileutils'
require 'tmpdir'
require 'yaml'
require 'pathname'
require 'benchmark'

def check_for_dpkg
  dpkg_installed = system 'which dpkg > /dev/null'
  unless dpkg_installed
    puts 'Package \'dpkg\' is required to verify debs. OSX users, try \'brew install dpkg\'. Exiting....'
    exit(1)
  end
end

def remove_old_debian_package(package_name)
  FileUtils.rm_rf(Dir.glob("#{PROJECT_ROOT}/artefacts/#{package_name}_*_amd64.deb"))
end

def debian_package_filepath(build_number, package_name)
  package_filename = debian_package_filename(build_number, package_name)
  File.expand_path("#{PROJECT_ROOT}/artefacts/#{package_filename}")
end

def debian_package_filename(build_number, package_name)
  "#{package_name}_#{build_number}_amd64.deb"
end

def build_debian_package(build_number, service_path, service_name, package_name)
  Dir.chdir("#{PROJECT_ROOT}/#{service_path}/build/install")
  FileUtils.rm_rf('deb/')
  FileUtils.mkdir_p("deb/ida/#{package_name}")
  FileUtils.mkdir_p("deb/opt/orch/#{service_name}")
  FileUtils.mkdir_p("deb/var/log/ida/debug")
  FileUtils.mkdir_p("deb/etc/logrotate.d")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/#{package_name}.yml", "deb/ida/#{package_name}/#{package_name}.yml")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/#{service_name}.sh", "deb/ida/#{package_name}/#{service_name}.sh")
  FileUtils.chmod("u=wrx,go=rx", "deb/ida/#{package_name}/#{service_name}.sh")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/orch-deploy", "deb/opt/orch/#{service_name}/deploy")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/orch-ready", "deb/opt/orch/#{service_name}/ready")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/orch-restart", "deb/opt/orch/#{service_name}/restart")
  FileUtils.cp("#{PROJECT_ROOT}/debian/#{package_name}/logrotate-console-log", "deb/etc/logrotate.d/#{service_name}")
  FileUtils.cp_r(service_name, "deb/ida/")

  package_file_name = debian_package_filepath(build_number, package_name)
  system "bundle exec fpm -C deb -s dir -t deb -n '#{package_name}' -v #{build_number} --deb-no-default-config-files --deb-systemd #{PROJECT_ROOT}/debian/#{service_name}/systemd/#{service_name}.service --deb-upstart #{PROJECT_ROOT}/debian/#{service_name}/upstart/#{service_name} --prefix=/ --after-install #{PROJECT_ROOT}/debian/#{package_name}/postinst.sh --depends python-httplib2 -p #{package_file_name} ."

  unless $? == 0
    raise 'fpm encountered an error'
  end
end

def verify_debian_package(package_file_path, package_identifier, install_prefix)
  puts "looking for: #{install_prefix}/#{package_identifier}/bin/#{package_identifier}"
  install_script_exists = system %Q(dpkg -c #{package_file_path} | grep '#{install_prefix}/#{package_identifier}/bin/#{package_identifier}$' > /dev/null)
  unless install_script_exists
    raise 'Invalid debian package structure.'
  end
end

def ensure_artefacts_directory_exists
  Dir.chdir(PROJECT_ROOT)
  FileUtils.mkdir_p('artefacts')
end

check_for_dpkg

system('bundle install --path vendor/bundle')

puts('Package and upload took: ' + Benchmark.realtime do
    service_map = YAML::load_file("#{PROJECT_ROOT}/bin/services.yaml")
    service_map.each do |package_name, attributes|
      service_path = attributes['path']
      puts "Building debian package: #{package_name}"
      service_name = service_path.split('/')[1]
      build_number = ENV.fetch('BUILD_NUMBER', '0')
      ensure_artefacts_directory_exists
      install_prefix = '/ida'
      remove_old_debian_package(package_name)
      build_debian_package(build_number, service_path, service_name, package_name)
      package_filepath = debian_package_filepath(build_number, package_name)
      verify_debian_package(package_filepath, package_name, install_prefix)
    end
end.to_s + ' seconds.')
