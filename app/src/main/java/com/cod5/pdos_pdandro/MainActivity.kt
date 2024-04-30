/*
 * 2022-10-25 PUBLIC DOMAIN  by Jean-Marc Lienher
 *
 * The authors disclam copyright to this source code
 */
package com.cod5.pdos_pdandro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.system.Os
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cod5.pdos_pdandro.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.util.*
import kotlin.math.ceil
import kotlin.math.log
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private var margin = 10
    private var line_height = 1F
    private var glyph_width = 1F
    private var console = Console()
    private lateinit var proc: Process
    private lateinit var wri: OutputStreamWriter
    private var input_buf = ""
    private lateinit var imm: InputMethodManager

    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
     }


    /* write data to the input of our native executable */
    fun write_buf() {
        try {
            if (input_buf.length > 0) {
                wri.append(input_buf)
                input_buf = ""
                wri.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* read data from our native executable and quit if it has exit */
    fun read_data() {
        try {
            val x = proc.exitValue()
            //finishAffinity()
            exitProcess(x)
            //System.exit(x)
            console.addtxt("EXIT", this)
        } catch (e: Exception) {
            // nothing
            //println("GO")
        }
        try {
            val a = proc.inputStream.available()
            if (a > 0) {
                val b = ByteArray(a)
                val t = proc.inputStream.read(b)
                console.addtxt(String(b), this)
            }
            val ae = proc.errorStream.available()
            if (ae > 0) {
                val be = ByteArray(ae)
                val te = proc.errorStream.read(be)
                console.addtxt(String(be), this)
            }
            if (a < 1 && ae < 1) {
                console.draw_cursor(this)
                return
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
    }
    /* draw the console characters */
    fun draw(draw_all: Boolean) {
        var i = 0
        var y = margin.toFloat() - paint.fontMetrics.ascent
        val ascent = paint.fontMetrics.ascent

        while (i < 25) {
            var j = 0
            val r = console.rows[i]
            var x = margin.toFloat()
            if (draw_all || i == console.cur_row) {
                while (j < 80) {
                    val col = r[j]
                    paint.color = col.bgcolor
                    canvas.drawRect(RectF(x, y + ascent, x + glyph_width, ceil(y + ascent + line_height)), paint)
                    x += glyph_width
                    j++
                }
            }
            y += line_height
            i++
        }
        i = 0
        y = margin.toFloat() - paint.fontMetrics.ascent
        while (i < 25) {
            var j = 0
            val r = console.rows[i]
            var x = margin.toFloat()
            if (draw_all || i == console.cur_row) {
                while (j < 80) {
                    val col = r[j]
                    paint.color = col.color
                    paint.typeface = col.typeface
                    canvas.drawText(col.txt, x, y, paint)
                    if (col.decoration.length > 0) {
                        canvas.drawText(col.decoration, x, y, paint)
                    }
                    x += glyph_width
                    j++
                }
            }
            y += line_height
            i++
        }
        binding.imageView?.invalidate()
    }

    /* physical keyboard input */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var s = when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> "\n"
            KeyEvent.KEYCODE_NUMPAD_ENTER -> "\n"
            KeyEvent.KEYCODE_ESCAPE -> "\u001b\u001b"
            KeyEvent.KEYCODE_DEL -> "\b"
            // https://sourceforge.net/p/pdos/gitcode/ci/master/tree/src/pdos.c#l1764
            KeyEvent.KEYCODE_DPAD_UP -> "\u001b[A"
            KeyEvent.KEYCODE_DPAD_DOWN -> "\u001b[B"
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.isCtrlPressed) {
                    "\u001b[1;5C"
                } else {
                    "\u001b[C"
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (event.isCtrlPressed) {
                    "\u001b[1;5D"
                } else {
                    "\u001b[D"
                }
            }
            KeyEvent.KEYCODE_INSERT -> "\u001b[2~"
            KeyEvent.KEYCODE_FORWARD_DEL -> "\u001b[3~"
            KeyEvent.KEYCODE_MOVE_HOME -> "\u001b[1~"
            KeyEvent.KEYCODE_MOVE_END -> "\u001b[4~"
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (event.isCtrlPressed) {
                    "\u001b[6;5~"
                } else {
                    "\u001b[6~"
                }
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                if (event.isCtrlPressed) {
                    "\u001b[5;5~"
                } else {
                    "\u001b[5~"
                }
            }
            else -> ""
        }
        if (event.isAltPressed) {
            if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                s = (keyCode - KeyEvent.KEYCODE_A + 'a'.code).toChar().toString()
                s = "\u001b$s"
            }
        }
        if (s.length == 0 && event.isCtrlPressed) {
            if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                s = (keyCode - KeyEvent.KEYCODE_A + 1).toChar().toString()
            }
        }
        if (s.length == 0) {
            val c = event.unicodeChar
            if (c >= ' '.code) {
                s = c.toChar().toString()
            }
        }
        if (s.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append(input_buf).append(s)
                input_buf = sb.toString()
                write_buf()
                return true
        } else {
                return super.onKeyDown(keyCode, event)
        }
    }

    /* timer to read data from our native executable */
    fun run_timer() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Handler(mainLooper).postDelayed(
                    { read_data() }, 0.toLong())
            }
        }, 1000, 20)
    }

    override fun onClick(p0: View?) {
        if (getCurrentFocus() == null && !imm.isAcceptingText) {
            imm.toggleSoftInputFromWindow(p0?.windowToken, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN, 0)
        }
    }

    /* called when the activity starts */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.imageView?.setOnClickListener(this)

        binding.imageView?.setOnKeyListener { view, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                onKeyDown(keyEvent.keyCode, keyEvent)
            }
            return@setOnKeyListener true
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()

        if (!hasWriteStoragePermission()) {
            return
        }

        if (!hasAllFilesPermission()) {
            askAllFilesPermission()
            return
        }

        run()
        run_timer()
        init_display()
    }

    /* initilise the console widget area */
    fun init_display()
    {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(dm)
        val dw = dm.widthPixels - 2 * margin
        val dh = dm.heightPixels - 2 * margin

        bitmap = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        paint = Paint()
        paint.color = Color.WHITE
        paint.strokeWidth = 0F
        paint.typeface = Console.normal

        // find the optimal text size for the screen
        var h = 0F
        paint.textSize = 8F
        var add = 1.0F
        while (h < dh) {
            h = (paint.fontMetrics.descent - paint.fontMetrics.ascent) * 25F
            if (h < dh) {
                paint.textSize += add
            } else {
                paint.textSize -= add
                if (add == 1.0F) {
                    add = 0.5F
                } else if (add == 0.5F) {
                    add = 0.125F
                } else {
                    h = dh.toFloat() + 1.0F
                }
            }
        }
        binding.imageView?.setImageBitmap(bitmap)
        line_height = paint.fontMetrics.descent - paint.fontMetrics.ascent
        glyph_width = paint.measureText("_")
        console.init()
    }



    /* print result of permission request */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun askAllFilesPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            if (hasAllFilesPermission()) {
                return true
            }

            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
         }
        return true
    }



    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasAllFilesPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Environment.isExternalStorageManager()
        }
        return true
    }


    /* asks for read/write permission on Download directory */
    private fun hasWriteStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return false
            }
        }
        return true
    }

    /* try to run exec */
    fun run() {
        val tim = Timer()
        tim.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Handler(mainLooper).postDelayed(
                    {
                        val ex = Environment.getExternalStorageDirectory().absolutePath
                        val dir = File("$ex/Download")
                        try {
                            if (!dir.exists()) {
                                dir.mkdir()
                            }
                        } catch (e: Exception) {
                            //
                        }
                        if (dir.isDirectory && dir.canWrite()) {
                            console.addtxt(System.getProperty("os.arch", "?") + " running in $dir\n",  this@MainActivity)
                            tim.cancel()
                            init_app(dir)
                        } else {
                            console.addtxt("$dir missing permission!\n", this@MainActivity)
                        }
                    }, 0.toLong())
            }
        }, 1000, 1000)
    }

    private fun isEqual(is1: InputStream, is2: InputStream) : Boolean {
        is1.use { src ->
            is2.use { dest ->
                var ch : Int
                while (src.read().also { ch = it } != -1) {
                    if (ch != dest.read()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /* copy files and run native executable */
    fun init_app(dir: File) {
        val s = applicationContext.applicationInfo.nativeLibraryDir
        val c = "$s/libpdos.so"

        if (dir.canWrite()) {
            c.runCommand(dir)
        }
    }


    /* execute system command */
    fun String.runCommand(workingDir: File): String {
        try {
            val own = applicationContext.applicationInfo.nativeLibraryDir
            Log.i(javaClass.name, own)
            proc = Runtime.getRuntime().exec(
                arrayOf<String> (this, "$own/lib%s.so", workingDir.absolutePath),
                Os.environ(), workingDir)
            wri = proc.outputStream.writer()
            Toast.makeText(applicationContext, "started", Toast.LENGTH_LONG).show();
            return ""
        } catch(e: IOException) {
            e.printStackTrace()
            return "ERROR"
        }
    }
}
