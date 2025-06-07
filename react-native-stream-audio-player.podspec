require 'json'


package = JSON.parse(File.read(File.join(__dir__, 'package.json')))


Pod::Spec.new do |spec|


  spec.name         = "react-native-stream-audio-player"
  spec.version      = package['version']
  spec.summary      = package["description"]
  spec.description  = package["description"]


  spec.author       = package["author"]
  spec.homepage     = package["homepage"]
  spec.license      = package["license"]
  spec.platform     = :ios, "13.0"
  spec.ios.deployment_target = "13.0"


  spec.source       = { :git => package['repository']['url'], :tag => "v#{package['version']}" }
  spec.source_files = "ios/*.{h,m,swift}"


  spec.dependency   "React"


end