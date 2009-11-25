/**
 * TODO(jsirois): provide a useful main that helps explore pe or else kill this - really just used to experiment with
 * buildr right now
 * 
 * @author john@siroisdesign.com
 */
object Main {
  def main (args: Array[String]) = {
    args.foreach(printf("Argument: %s\n", _))
    println
  }
}