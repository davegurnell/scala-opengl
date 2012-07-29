package example2

import com.jme3.math._
import java.nio._
import org.lwjgl._
import org.lwjgl.BufferUtils._
import org.lwjgl.input._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._

/*
Display a quad on-screen. This code uses:

 - a simple vertex and fragment shader;
 - a vertex array;
 - a vertex buffer containing interleaved positions and colours;
 - glDrawArrays to draw the vertex array.

Simplifications:

 - the shaders ignore the Z coordinate, so what you're seeing is effectively 2D;
 - the shaders don't use uniforms, so you can't easily change the scene each frame.
*/

object Example {
  val width = 600
  val height = 600

  val POS_INDEX = 0
  val COL_INDEX = 1

  var programName = 0
  var vertexShaderName = 0
  var fragmentShaderName = 0

  var vaoName = 0
  var vboName = 0

  val FLOAT_BYTES = java.lang.Float.SIZE   / java.lang.Byte.SIZE
  val INT_BYTES   = java.lang.Integer.SIZE / java.lang.Byte.SIZE
  val VEC4_BYTES  = 4 * FLOAT_BYTES

  val vertexShader =
    """
    #version 150

    in vec4 in_Position;
    in vec4 in_Color;
    out vec4 ex_Color;

    void main(void)
    {
      gl_Position = in_Position;
      ex_Color = in_Color;
    }
    """

  val fragmentShader =
    """
    #version 150

    in vec4 ex_Color;
    out vec4 out_Color;

    void main(void)
    {
       out_Color = ex_Color;
    }
    """

  val vertices = Array[Float](
    // Bottom right triangle
    -0.8f, -0.8f,  0.0f,  1.0f, /* bottom left vertex  */ 1.0f,  0.0f,  0.0f,  1.0f, /* bottom left color  - red     */
     0.8f, -0.8f,  0.0f,  1.0f, /* bottom right vertex */ 0.0f,  1.0f,  0.0f,  1.0f, /* bottom right color - green   */
     0.8f,  0.8f,  0.0f,  1.0f, /* top right vertex    */ 0.0f,  0.0f,  1.0f,  1.0f, /* top right color    - blue    */
    // Top left triangle
    -0.8f, -0.8f,  0.0f,  1.0f, /* bottom left vertex  */ 1.0f,  1.0f,  0.0f,  1.0f, /* bottom left color  - yellow  */
     0.8f,  0.8f,  0.0f,  1.0f, /* top right vertex    */ 1.0f,  0.0f,  1.0f,  1.0f, /* top right color    - magenta */
    -0.8f,  0.8f,  0.0f,  1.0f, /* top left vertex     */ 0.0f,  1.0f,  1.0f,  1.0f  /* top left color     - cyan    */
  )

  def main(args: Array[String]): Unit = {
    try {
      createDisplay()
      createKeyboard()
      createShaders()
      createVBO()

      while(!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
        frame()
      }
    } catch {
      case exn => exn.printStackTrace()
    } finally {
      destroyVBO()
      destroyShaders()
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

    Display.setDisplayMode(new DisplayMode(width, height))
    Display.setFullscreen(false)
    Display.setTitle("Title")
    Display.create(pixelFormat, contextAttributes)
    glGetError()

    println("OS name " + System.getProperty("os.name"))
    println("OS version " + System.getProperty("os.version"))
    println("LWJGL version " + org.lwjgl.Sys.getVersion())
    println("OpenGL version " + glGetString(GL_VERSION))
    Util.checkGLError()

    glClearColor(0, 0, 0, 0)
    Util.checkGLError()

    glDisable(GL_DEPTH_TEST)
    Util.checkGLError()

    glViewport(0, 0, width, height)
    Util.checkGLError()
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

  def createShaders(): Unit = {
    vertexShaderName = glCreateShader(GL_VERTEX_SHADER)
    Util.checkGLError()

    glShaderSource(vertexShaderName, vertexShader)
    Util.checkGLError()

    glCompileShader(vertexShaderName)
    Util.checkGLError()

    println(shaderLog("Vertex shader ", vertexShaderName))
    Util.checkGLError()

    fragmentShaderName = glCreateShader(GL_FRAGMENT_SHADER)
    Util.checkGLError()

    glShaderSource(fragmentShaderName, fragmentShader)
    Util.checkGLError()

    glCompileShader(fragmentShaderName)
    Util.checkGLError()

    println(shaderLog("Fragment shader ", fragmentShaderName))
    Util.checkGLError()

    programName = glCreateProgram()
    Util.checkGLError()

    glAttachShader(programName, vertexShaderName)
    Util.checkGLError()

    glAttachShader(programName, fragmentShaderName)
    Util.checkGLError()

    glBindAttribLocation(programName, POS_INDEX, "in_Position")
    Util.checkGLError()

    glBindAttribLocation(programName, COL_INDEX, "in_Color")
    Util.checkGLError()

    glLinkProgram(programName)
    Util.checkGLError()

    println(programLog("Program ", programName))
    Util.checkGLError()

    glUseProgram(programName)
    Util.checkGLError()
  }

  def destroyShaders(): Unit = {
    glUseProgram(0)
    Util.checkGLError()

    glDetachShader(programName, fragmentShaderName)
    Util.checkGLError()

    glDetachShader(programName, vertexShaderName)
    Util.checkGLError()

    glDeleteShader(fragmentShaderName)
    Util.checkGLError()

    glDeleteShader(vertexShaderName)
    Util.checkGLError()

    glDeleteProgram(programName)
    Util.checkGLError()
  }

  def createVBO(): Unit = {
    vaoName = glGenVertexArrays()
    Util.checkGLError()

    glBindVertexArray(vaoName)
    Util.checkGLError()

    glEnableVertexAttribArray(POS_INDEX)
    Util.checkGLError()

    glEnableVertexAttribArray(COL_INDEX)
    Util.checkGLError()

    // Vertices and colours:

    val verticesFB = createFloatBuffer(vertices.length)
    verticesFB.put(vertices)
    verticesFB.rewind()
    Util.checkGLError()

    vboName = glGenBuffers()
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, vboName)
    Util.checkGLError()

    glBufferData(GL_ARRAY_BUFFER, verticesFB, GL_STATIC_DRAW)
    Util.checkGLError()

    glVertexAttribPointer(POS_INDEX, 4, GL_FLOAT, false, 2 * VEC4_BYTES, 0)
    Util.checkGLError()

    glVertexAttribPointer(COL_INDEX, 4, GL_FLOAT, false, 2 * VEC4_BYTES, VEC4_BYTES)
    Util.checkGLError()

    // Clean up (sanity check):

    glBindVertexArray(0)
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    Util.checkGLError()
  }

  def destroyVBO(): Unit = {
    glDisableVertexAttribArray(POS_INDEX)
    Util.checkGLError()

    glDisableVertexAttribArray(COL_INDEX)
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    Util.checkGLError()

    glDeleteBuffers(vboName)
    Util.checkGLError()

    glBindVertexArray(0)
    Util.checkGLError()

    glDeleteVertexArrays(vaoName)
    Util.checkGLError()
  }

  def frame(): Unit = {
    if(Display.isVisible()) {
      render()
    } else {
      if(Display.isDirty()) {
        render()
      }
      try {
        Thread.sleep(100)
      } catch {
        case exn: InterruptedException => // Do nothing
      }
    }

    Display.update()
    Display.sync(60)
  }

  def render(): Unit = {
    println("GLTest.render")

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    Util.checkGLError()

    glBindVertexArray(vaoName)
    Util.checkGLError()

    // glValidateProgram(programName)
    // Util.checkGLError()

    // println("programName " + programName)
    // println("vertexShaderName " + vertexShaderName)
    // println("fragmentShaderName " + fragmentShaderName)
    // println("vaoName " + vaoName)
    // println("vboName " + vboName)
    // println("indicesName " + indicesName)
    // println("current program " + GL11.glGetInteger(GL_CURRENT_PROGRAM))
    // println("program link status " + glGetProgram(programName, GL_LINK_STATUS))
    // println("program delete status " + glGetProgram(programName, GL_DELETE_STATUS))
    // println("program validate status " + glGetProgram(programName, GL_VALIDATE_STATUS))
    // println("program attached shaders " + glGetProgram(programName, GL_ATTACHED_SHADERS))
    // println("program active attributes " + glGetProgram(programName, GL_ACTIVE_ATTRIBUTES))
    // println("program active uniforms " + glGetProgram(programName, GL_ACTIVE_UNIFORMS))
    // println("vao is a vao " + glIsVertexArray(vaoName))

    glDrawArrays(GL_TRIANGLES, 0, 6)
    Util.checkGLError()
  }

  // Debugging code:

  private def shaderLog(preamble: String, location: Int): String = {
    val len = glGetShader(location, GL_INFO_LOG_LENGTH)
    val log = glGetShaderInfoLog(location, len)
    formatLog(preamble, len, log)
  }

  private def programLog(preamble: String, location: Int): String = {
    val len = glGetProgram(location, GL_INFO_LOG_LENGTH)
    val log = glGetProgramInfoLog(location, len)
    formatLog(preamble, len, log)
  }

  private def formatLog(preamble: String, len: Int, log: String): String = {
    var ans = ""
    ans += preamble + "(length " + len + ") {\n"
    for(line <- log.split("\n")) {
      ans += "  " + line + "\n"
    }
    ans += "}\n"
    ans
  }
}
