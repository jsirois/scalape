ENV['USE_FSC'] ||= 'yes'

VERSION_NUMBER = "1.0.0"
GROUP = "scalape"
COPYRIGHT = """
C #{Time.new.year} John Sirois
"""

repositories.remote << "http://www.ibiblio.org/maven2/"

require 'buildr/scala'

desc "The Scalape project"
define "scalape" do
  project.version = VERSION_NUMBER
  project.group = GROUP

  manifest["Implementation-Vendor"] = COPYRIGHT

  compile.with # Add classpath dependencies

  test.compile.with # Add classpath dependencies
  test.using(:scalatest)

  package(:jar).with :manifest=>{ 'Main-Class'=>'Main' }
end
