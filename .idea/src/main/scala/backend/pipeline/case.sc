case class Test() {
  def this(id: Int) = this()

}

class TestClass(name: String) {
  def this(id: Int) = this(id.toString)

}
new TestClass( 1)

val test = new Test(1)
test.