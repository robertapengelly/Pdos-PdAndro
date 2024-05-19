package com.cod5.pdos_pdandro

import  android.Manifest
import  android.app.Activity
import  android.content.Intent
import  android.content.pm.PackageManager
import  android.graphics.Bitmap
import  android.graphics.Canvas
import  android.graphics.Color
import  android.graphics.Paint
import  android.graphics.RectF
import  android.net.Uri
import  android.os.Build
import  android.os.Bundle
import  android.os.Environment
import  android.os.Handler
import  android.provider.Settings
import  android.view.KeyEvent
import  android.system.Os
import  android.util.DisplayMetrics
import  android.widget.Toast

import  com.cod5.pdos_pdandro.databinding.ActivityMainBinding

import  java.io.File
import  java.io.OutputStreamWriter
import  java.util.Timer
import  java.util.TimerTask

import  kotlin.math.ceil
import  kotlin.system.exitProcess

class MainActivity : Activity () {

    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
    }
    
    private lateinit var binding: ActivityMainBinding
    private var input_buf = ""
    
    private var isRunning = false
    private var margin = 10
    
    private var line_height = 1f
    private var glyph_width = 1f
    
    private var console = Console ()
    private var timer = Timer ()
    
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var proc: Process
    private lateinit var wri: OutputStreamWriter
    
    private fun hasAllFilesPermission (): Boolean {
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager ()
        }
        
        return true
    
    }
    
    private fun hasWriteStoragePermission (): Boolean {
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return applicationContext.checkCallingOrSelfPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        return true
    
    }
    
    private fun askWritePermission () {
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissions (arrayOf (Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    
    }
    
    private fun askAllFilesPermission () {
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        
            val uri = Uri.parse ("package:${BuildConfig.APPLICATION_ID}")
            startActivity (Intent (Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
        
        }
    
    }
    
    /* run native executable */
    private fun init_app (dir: File) {
    
        val s = applicationContext.applicationInfo.nativeLibraryDir
        val c = "$s/libpdos.so"
        
        if (dir.canWrite ()) {
            c.runCommand (dir)
        }
    
    }
    
    /* timer to read data from our native executable */
    fun run_timer () {
    
        Timer ().schedule (object : TimerTask () {
        
            override fun run () {
                Handler (mainLooper).postDelayed ({ read_data() }, 0.toLong())
            }
        
        }, 1000, 20)
    
    }
    
    private fun run () {
    
        timer.schedule (object: TimerTask () {
        
            override fun run () {
            
                Handler (mainLooper).postDelayed ({
                
                    val ex = Environment.getExternalStorageDirectory ().absolutePath
                    val dir = File ("$ex/Download")
                    
                    try {
                    
                        if (!dir.exists ()) {
                            dir.mkdir ()
                        }
                    
                    } catch (_: Exception) {}
                    
                    if (dir.isDirectory && dir.canWrite ()) {
                    
                        timer.cancel ()

                        System.getProperty ("os.arch", "?")?.plus (" running in $dir\n")?.let { console.addtxt (it, this@MainActivity) }
                        init_app (dir)
                    
                    } else {
                        console.addtxt ("$dir missing permission!\n", this@MainActivity)
                    }
                
                }, 0.toLong ())
            
            }
        
        }, 1000, 1000)
    
    }
    
    /* write data to the input of our native executable */
    private fun write_buf() {
    
        try {
        
            if (input_buf.length > 0) {
            
                wri.append (input_buf)
                input_buf = ""
                
                wri.flush ()
            
            }
        
        } catch (e: Exception) {
            e.printStackTrace ()
        }
    
    }
    
    /* read data from our native executable and quit if it has exit */
    private fun read_data () {
    
        try {
        
            val x = proc.exitValue ()
            exitProcess (x)
        
        } catch (_: Exception) {}
        
        try {
        
            val a = proc.inputStream.available ()
            
            if (a > 0) {
            
                val b = ByteArray (a)
                proc.inputStream.read (b)
                
                console.addtxt (String (b), this)
            
            }
            
            val ae = proc.errorStream.available ()
            
            if (ae > 0) {
            
                val be = ByteArray (ae)
                proc.errorStream.read (be)
                
                console.addtxt (String (be), this)
            
            }
            
            if (a < 1 && ae < 1) {
                console.draw_cursor (this)
            }
        
        } catch (_: Exception) {}
    
    }
    
    @Suppress ("DEPRECATION")
    private fun init_display () {
    
        val dm = DisplayMetrics ()
        windowManager.defaultDisplay.getMetrics (dm)
        
        val dw = dm.widthPixels - (2 * margin)
        val dh = dm.heightPixels - (2 * margin)
        
        bitmap = Bitmap.createBitmap (dw, dh, Bitmap.Config.ARGB_8888)
        canvas = Canvas (bitmap)
        
        paint = Paint ()
        paint.typeface = Console.normal
        
        paint.color = Color.WHITE
        paint.strokeWidth = 0f
        
        // find the optimal text size for the screen
        var h = 0f
        paint.textSize = 8f
        
        var add = 1.0f
        
        while (h < dh) {
        
            h = (paint.fontMetrics.descent - paint.fontMetrics.ascent) * 25f
            
            if (h < dh) {
                paint.textSize += add
            } else {
            
                paint.textSize -= add
                
                if (add == 1.0f) {
                    add = 0.5f
                } else if (add == 0.5f) {
                    add = 0.125f
                } else {
                    h = dh.toFloat () + 1.0f
                }
            
            }
        
        }
        
        binding.imageview.setImageBitmap (bitmap)
        
        line_height = paint.fontMetrics.descent - paint.fontMetrics.ascent
        glyph_width = paint.measureText ("_")
        
        console.init ()
    
    }
    
    fun draw (draw_all: Boolean) {
    
        val ascent = paint.fontMetrics.ascent
        var y = margin.toFloat () - paint.fontMetrics.ascent
        
        for (i in 0 until 25) {
        
            val r = console.rows[i]
            var x = margin.toFloat ()
            
            if (draw_all || i == console.cur_row) {
            
                for (j in 0 until 80) {
                
                    val col = r[j]
                    
                    paint.color = col.backgroundColor
                    paint.typeface = col.typeface
                    
                    canvas.drawRect (RectF (x, y + ascent, x + glyph_width, ceil (y + ascent + line_height)), paint)
                    x += glyph_width
                
                }
            
            }
            
            y += line_height
        
        }
        
        y = margin.toFloat () - paint.fontMetrics.ascent
        
        for (i in 0 until 25) {
        
            val r = console.rows[i]
            var x = margin.toFloat ()
            
            if (draw_all || i == console.cur_row) {
            
                for (j in 0 until 80) {
                
                    val col = r[j]
                    
                    paint.color = col.foregroundColor
                    paint.typeface = col.typeface
                    
                    canvas.drawText (col.txt, x, y, paint)
                    
                    if (col.decoration.length > 0) {
                        canvas.drawText (col.decoration, x, y, paint)
                    }
                    
                    x += glyph_width
                
                }
            
            }
            
            y += line_height
        
        }
        
        binding.imageview.invalidate ()
    
    }
    
    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate (savedInstanceState)
        
        binding = ActivityMainBinding.inflate (layoutInflater)
        setContentView (binding.root)
        
        init_display ()
    
    }
    
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
            
                s = (keyCode - KeyEvent.KEYCODE_A + 'a'.code).toChar ().toString ()
                s = "\u001b$s"
            
            }
        
        }
        
        if (s.length == 0 && event.isCtrlPressed) {
        
            if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                s = (keyCode - KeyEvent.KEYCODE_A + 1).toChar ().toString ()
            }
        
        }
        
        if (s.length == 0) {
        
            val c = event.unicodeChar
            
            if (c >= ' '.code) {
                s = c.toChar ().toString ()
            }
        
        }
        
        if (s.isNotEmpty()) {
        
            val sb = StringBuilder ()
            sb.append (input_buf).append (s)
            
            input_buf = sb.toString ()
            write_buf ()
            
            return true
        
        } else {
            return super.onKeyDown (keyCode, event)
        }
    
    }
    
    override fun onResume () {
        super.onResume ()
        
        if (!isRunning) {
        
            if (!hasWriteStoragePermission ()) {
            
                askWritePermission ()
                return
            
            }
            
            if (!hasAllFilesPermission ()) {
            
                askAllFilesPermission ()
                return
            
            }
            
            run ()
            run_timer ()
        
        }
    
    }
    
    override fun onRequestPermissionsResult (requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
        
            if (grantResults.isNotEmpty () && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText (this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT).show ()
            } else {
                Toast.makeText (this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT).show ()
            }
        
        }
    
    }
    
    /* execute system command */
    fun String.runCommand (workingDir: File) {
    
        try {
        
            val own = applicationContext.applicationInfo.nativeLibraryDir
            android.util.Log.i (javaClass.name, own)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                proc = Runtime.getRuntime ().exec (arrayOf<String> (this, "$own/lib%s.so", workingDir.absolutePath), Os.environ (), workingDir)
            } else {
                proc = Runtime.getRuntime ().exec (arrayOf<String> (this, "$own/lib%s.so", workingDir.absolutePath), arrayOf<String> (), workingDir)
            }
            
            wri = proc.outputStream.writer ()
            
            isRunning = true
            Toast.makeText (applicationContext, "started", Toast.LENGTH_LONG).show ()
        
        } catch(e: Exception) {
            e.printStackTrace ()
        }
    
    }

}