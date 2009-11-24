/**
 * @author john@siroisdesign.com
 */
object Main {
  def main (args: Array[String]) = {
    args.foreach(printf("Argument: %s\n", _))
    println
  }
}