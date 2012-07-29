package example5

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
 - two vertex buffers, one containing positions and one containing colours;
 - an index buffer;
 - glDrawElements to draw the array using the index buffer;
 - a uniform to control the world transform of the quad.

Simplifications:

 - the shaders ignore the Z coordinate, so what you're seeing is effectively 2D;
 - the shaders don't use uniforms, so you can't easily change the scene each frame.
*/

object Example {
  val WIDTH = 600
  val HEIGHT = 600

  val START_TIME = System.currentTimeMillis

  val POS_INDEX = 0
  val COL_INDEX = 1

  var programName = 0
  var vertexShaderName = 0
  var fragmentShaderName = 0
  var projectionMatrixName = 0
  var viewMatrixName = 0

  var vaoName = 0
  var vboName = 0
  var colName = 0
  var indicesName = 0

  var cameraX = 0f
  var cameraY = 0f
  var cameraZ = 1f
  var modelYaw = (math.Pi / 4).toFloat

  val FLOAT_BYTES = java.lang.Float.SIZE / java.lang.Byte.SIZE
  val INT_BYTES   = java.lang.Integer.SIZE / java.lang.Byte.SIZE
  val VEC4_BYTES  = 4 * FLOAT_BYTES

  val vertexShader =
    """
    #version 150

    uniform mat4 g_viewMatrix;
    uniform mat4 g_projectionMatrix;

    in vec4 in_Position;
    in vec4 in_Color;
    out vec4 ex_Color;

    void main(void)
    {
      gl_Position = g_projectionMatrix * (g_viewMatrix * in_Position);
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
    -0.8f, -0.8f,  0.0f,  1.0f, /* bottom left vertex  */
     0.8f, -0.8f,  0.0f,  1.0f, /* bottom right vertex */
     0.8f,  0.8f,  0.0f,  1.0f, /* top right vertex    */
    -0.8f,  0.8f,  0.0f,  1.0f  /* top left vertex     */
  )

  val colors = Array[Float](
    1.0f,  0.0f,  0.0f,  1.0f, /* bottom left color  - red    */
    0.0f,  1.0f,  0.0f,  1.0f, /* bottom right color - green  */
    0.0f,  0.0f,  1.0f,  1.0f, /* top right color    - blue   */
    1.0f,  1.0f,  0.0f,  1.0f  /* top left color     - yellow */
  )

  val indices = Array[Int](
    0, 1, 2,
    0, 2, 3
  )

  val identityMatrix = Array[Float](
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
  )

  val projectionMatrix = Array[Float](
    1f,  0f,  0f,  0f,
    0f,  1f,  0f,  0f,
    0f,  0f,  1f,  0f,
    0f,  0f, -1f,  0f
  )

  def rotationMatrix(yaw: Float) = Array[Float](
     math.cos(yaw).toFloat, 0f, -math.sin(yaw).toFloat, 0f,
     0f,                    1f,                     0f, 0f,
    -math.sin(yaw).toFloat, 0f,  math.cos(yaw).toFloat, 0f,
     0f,                    0f,                     0f, 1f
  )

  def translationMatrix(x: Float, y: Float, z: Float) = Array[Float](
    1f, 0f, 0f, -x,
    0f, 1f, 0f, -y,
    0f, 0f, 1f, -z,
    0f, 0f, 0f, 1f
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

    Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT))
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

    glViewport(0, 0, WIDTH, HEIGHT)
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

    viewMatrixName = glGetUniformLocation(programName, "g_viewMatrix")
    Util.checkGLError()

    projectionMatrixName = glGetUniformLocation(programName, "g_projectionMatrix")
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

    // Vertices:

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

    glVertexAttribPointer(POS_INDEX, 4, GL_FLOAT, false, 0, 0)
    Util.checkGLError()

    // Colors:

    val colorsFB = createFloatBuffer(colors.length)
    colorsFB.put(colors)
    colorsFB.rewind()
    Util.checkGLError()

    colName = glGenBuffers()
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, colName)
    Util.checkGLError()

    glBufferData(GL_ARRAY_BUFFER, colorsFB, GL_STATIC_DRAW)
    Util.checkGLError()

    glVertexAttribPointer(COL_INDEX, 4, GL_FLOAT, false, 0, 0)
    Util.checkGLError()

    // Indices:

    val indicesFB = createIntBuffer(indices.length)
    indicesFB.put(indices)
    indicesFB.rewind()
    Util.checkGLError()

    indicesName = glGenBuffers()
    Util.checkGLError()

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesName)
    Util.checkGLError()

    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesFB, GL_STATIC_DRAW)
    Util.checkGLError()

    // Clean up (sanity check):

    glBindVertexArray(0)
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    Util.checkGLError()

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    Util.checkGLError()
  }

  def destroyVBO(): Unit = {
    glDisableVertexAttribArray(POS_INDEX)
    Util.checkGLError()

    glDisableVertexAttribArray(COL_INDEX)
    Util.checkGLError()

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    Util.checkGLError()

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    Util.checkGLError()

    glDeleteBuffers(indicesName)
    Util.checkGLError()

    glDeleteBuffers(vboName)
    Util.checkGLError()

    glDeleteBuffers(colName)
    Util.checkGLError()

    glBindVertexArray(0)
    Util.checkGLError()

    glDeleteVertexArrays(vaoName)
    Util.checkGLError()
  }

  def frame(): Unit = {
    if(Display.isVisible()) {
      handleKeyboardInput()
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

  def handleKeyboardInput(): Unit = {
    if(Keyboard.isKeyDown(Keyboard.KEY_UP   )) cameraZ -= (1f / 60f)
    if(Keyboard.isKeyDown(Keyboard.KEY_DOWN )) cameraZ += (1f / 60f)
    if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
      if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        cameraX -= (1f / 60f)
      } else {
        modelYaw += (1f / 60f)
      }
    }
    if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
      if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        cameraX += (1f / 60f)
      } else {
        modelYaw -= (1f / 60f)
      }
    }
  }

  def render(): Unit = {
    // println("GLTest.render: camera = (%.2f, %.2f, %.2f)".format(cameraX, cameraY, cameraZ))

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    Util.checkGLError()

    glBindVertexArray(vaoName)
    Util.checkGLError()

    glUniformMatrix4(projectionMatrixName, false, matrixToBuffer(projectionMatrix))
    glUniformMatrix4(
      viewMatrixName,
      false,
      matrixToBuffer(multiply(
        translationMatrix(cameraX, cameraY, cameraZ),
        rotationMatrix(modelYaw)
      ))
    )

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

    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
    Util.checkGLError()
  }

  private def matrixToBuffer(in: Array[Float]): FloatBuffer = {
    val ans = BufferUtils.createFloatBuffer(16)

    for {
      i <- 0 until 4
      j <- 0 until 4
    } ans.put(in(4*j + i))

    ans.rewind()

    ans
  }

  private def multiply(a: Array[Float], b: Array[Float]): Array[Float] = {
    val ans = new Array[Float](16)

    for {
      j <- 0 until 4
      i <- 0 until 4
    } {
      ans(4*j + i) =
        a(4*j + 0) * b(4*0 + i) +
        a(4*j + 1) * b(4*1 + i) +
        a(4*j + 2) * b(4*2 + i) +
        a(4*j + 3) * b(4*3 + i)
    }

    ans
  }

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
