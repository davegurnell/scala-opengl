package example1

import java.nio._
import org.lwjgl._
import org.lwjgl.BufferUtils._
import org.lwjgl.input._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.Util._

/*
Create an empty LWJGL window and wait for the user to quit the application.
*/

object Example {
  val WIDTH  = 600
  val HEIGHT = 600

  def main(args: Array[String]): Unit = {
    try {
      createDisplay()
      createKeyboard()

      while(!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
        // Insert demo here
      }
    } catch {
      case exn => exn.printStackTrace()
    } finally {
      destroyKeyboard()
      destroyDisplay()
    }
  }

  def createDisplay(): Unit = {
    val pixelFormat = new PixelFormat()

    val contextAttributes = new ContextAttribs(3, 2).
      withProfileCompatibility(false).
      withForwardCompatible(true).
      withProfileCore(true)

    Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT))
    Display.setFullscreen(false)
    Display.setTitle("Example")
    Display.create(pixelFormat, contextAttributes)
    glGetError()

    println("OS name " + System.getProperty("os.name"))
    println("OS version " + System.getProperty("os.version"))
    println("LWJGL version " + org.lwjgl.Sys.getVersion())
    println("OpenGL version " + glGetString(GL_VERSION))
    checkGLError()

    glClearColor(0, 0, 0, 0)
    checkGLError()

    glDisable(GL_DEPTH_TEST)
    checkGLError()

    glViewport(0, 0, WIDTH, HEIGHT)
    checkGLError()
  }

  def destroyDisplay(): Unit = {
    Display.destroy()
  }

  def createKeyboard(): Unit = {
    Keyboard.create()
  }

  def destroyKeyboard(): Unit = {
    Keyboard.destroy()
  }
}
