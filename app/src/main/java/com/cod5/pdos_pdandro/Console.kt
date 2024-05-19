package com.cod5.pdos_pdandro

import  android.graphics.Color
import  android.graphics.Typeface

class Console {

    companion object{
    
        var normal: Typeface = Typeface.create("monospace", Typeface.NORMAL)
        var bold: Typeface = Typeface.create("monospace", Typeface.BOLD)
        var italic: Typeface = Typeface.create("monospace", Typeface.ITALIC)
        var bolditalic: Typeface = Typeface.create("monospace", Typeface.BOLD_ITALIC)
    
    }
    
    private var cur_show = true
    private var cur_timer = 0
    
    private var escape_state = 0
    private var escape_val = 0
    
    private var escape_args = arrayListOf<Int> ()
    private var escape_save = arrayListOf<Int> ()
    
    private var escape_gfx = ScreenChar ("")
    
    var cur_row = 0
    var cur_col = 0
    
    var rows = arrayListOf<ScreenRow> ()
    
    /* scroll the console rows */
    fun scroll (amount: Int) {
    
        var i = 0
        
        if (amount < 0) {
        
            while (i > amount) {
                rows.removeLast ()
                i--
            }
            
            i = 0
            
            while (i > amount) {
            
                rows.add(0, ScreenRow (80))
                rows[0].set (escape_gfx)
                
                i--
            
            }
        
        } else {
        
            while (i < amount) {
            
                rows.removeAt (0)
                i++
            
            }
            
            i = 0
            
            while (i < amount) {
            
                rows.add (ScreenRow(80))
                rows[rows.size-1].set (escape_gfx)
                
                i++
            
            }
        
        }
    
    }
    
    /* blank the character in the range */
    private fun earse (start_col : Int, start_row :Int, end_col: Int, end_row:Int) {
    
        var r = start_row
        
        while (r <= end_row) {
        
            val row = rows[r]
            
            var c = 0
            var ec = 79
            
            if (r == start_row ) {
                c = start_col
            }
            
            if (r == end_row) {
                ec = end_col
            }
            
            while (c <= ec) {
            
                row[c].txt = ""         // TODO
                c++
            
            }
            
            r++
        
        }
    
    }
    
    /* 8 colors for the ANSI console */
    private fun get_ansi_color (index: Int) : Int {
    
        return when (index) {
        
            0 -> Color.BLACK
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.YELLOW
            4 -> Color.BLUE
            5 -> Color.MAGENTA
            6 -> Color.CYAN
            7 -> Color.WHITE
            
            else -> Color.GRAY
        
        }
    
    }
    
    // Select Graphics Rendition
    // https://en.wikipedia.org/wiki/ANSI_escape_code
    private fun sgr () {
    
        var f = 0
        
        if (escape_args.size > 1) {
            f = escape_args[0]
        }
        
        if (f == 0) {
            escape_gfx.reset ()
        } else if (f == 1) {
        
            if (escape_gfx.typeface == italic) {
                escape_gfx.typeface = bolditalic
            } else {
                escape_gfx.typeface = bold
            }
        
        } else if (f == 2) {
            // do nothing
        } else if (f == 3) {
        
            if (escape_gfx.typeface == bold) {
                escape_gfx.typeface = bolditalic
            } else {
                escape_gfx.typeface = italic
            }
        
        } else if (f == 4) {
            escape_gfx.decoration = "_"
        } else if (f >= 30 && f <= 37) {
            escape_gfx.foregroundColor = get_ansi_color (f - 30)
        } else if (f >= 40 && f <= 47) {
            escape_gfx.backgroundColor = get_ansi_color (f - 40)
        } else if (f >= 90 && f <= 97) {
            escape_gfx.foregroundColor = get_ansi_color (f - 90)                // FIXME brighter
        } else if (f >= 100 && f <= 107) {
            escape_gfx.backgroundColor = get_ansi_color (f - 100)               // FIXME brighter
        }
    
    }
    
    /* private escape sequence */
    private fun ansi_private (c : Int) {
    
        if (c == 'h'.code) {            // show caret
        
            if (escape_args.size > 1) {
            
                val cmd = escape_args[1]
                
                if (cmd == 25) {
                    cur_show = true
                }
            
            }
        
        } else if (c == 'l'.code) { // hide caret
        
            if (escape_args.size > 1) {
            
                val cmd = escape_args[1]
                
                if (cmd == 25) {
                    cur_show = false
                }
            
            }
        
        }
    
    }
    
    /* parse escape sequences */
    private fun ansi_term (c: Int) {
    
        // https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797
        // https://sourceforge.net/p/pdos/gitcode/ci/master/tree/src/pdos.c#l5493
        var n = 1
        
        if (escape_args.size > 0) {
        
            if (escape_args[0] == -'?'.code) {
            
                ansi_private (c)
                return
            
            }
            
            n = escape_args[0]
        
        }
        
        if (c == 'A'.code) {
            cur_row -= n
        } else if (c == 'B'.code) {
            cur_row += n
        } else if (c == 'C'.code) {
            cur_col += n
        } else if (c == 'D'.code) {
            cur_col -= n
        } else if (c == 'E'.code) {
        
            cur_row += n
            cur_col = 0
        
        } else if (c == 'F'.code) {
        
            cur_row -= n
            cur_col = 0
        
        } else if (c == 'G'.code || c == 'f'.code) {
        
            if (escape_args.size > 0) {
                cur_col = escape_args[0] - 1
            }
        
        } else if (c == 'H'.code) { // set cursor position
        
            if (escape_args.size > 0) {
            
                cur_row = escape_args[0] - 1
                
                if (escape_args.size > 1) {
                    cur_col = escape_args[1] - 1
                } else {
                    cur_col = 0
                }
            
            }
        
        } else if (c == 'J'.code) {     // clear screen
        
            if (escape_args.size == 0) {
                n = 0
            }
            
            if (n == 0) {
                earse (cur_col, cur_row, 79, 24)
            } else if (n == 1) {
                earse (0, 0, cur_col, cur_row)
            } else if (n == 2) {
                earse (0, 0, 79, 24)
            }
        
        } else if (c == 'K'.code) {     // clear line
        
            if (escape_args.size == 0) {
                n = 0
            }
            
            if (n == 0) {
                earse (cur_col, cur_row, 79, cur_row)
            } else if (n == 1) {
                earse (0, cur_row, cur_col, cur_row)
            } else if (n == 2) {
                earse (0, cur_row, 79, cur_row)
            }
        
        } else if (c == 'S'.code) {
            scroll (n)
        } else if (c == 'T'.code) {
            scroll (-n)
        } else if (c == 's'.code) {
        
            escape_save.add (cur_row)
            escape_save.add (cur_col)
        
        } else if (c == 'u'.code) {
        
            if (escape_save.size > 1) {
            
                cur_col = escape_save.removeLast ()
                cur_row = escape_save.removeLast ()
            
            }
        
        } else if (c == 'm'.code) {
            sgr ()
        }
    
    }
    
    fun init () {
    
        for (i in 0 until 25) {
            rows.add (ScreenRow (80))
        }
    
    }
    
    /* append text to our ANSI console */
    fun addtxt (txt: String, activity: MainActivity) {
    
        var all = false
        var i = 0
        
        val nbcp = txt.codePointCount (0, txt.length)
        
        var row = rows[cur_row]
        var col = row[cur_col]
        
        var last_row = cur_row
        var last_col = cur_col
        
        while (i < nbcp) {
        
            val c = txt.codePointAt (i)
            
            if (escape_state == 3) {
                escape_state = 0
            }
            
            if (escape_state == 1) {
            
                // https://notes.burke.libbey.me/ansi-escape-codes/
                if (c == '['.code) {
                
                    escape_state = 2
                    escape_val = -1
                    
                    escape_args.clear ()
                
                } else {
                
                    escape_state = 0
                    i--                 // rescan the current character
                
                }
            
            } else if (escape_state == 2) {
            
                if (c >= '0'.code && c <= '9'.code) {
                
                    if (escape_val == -1) {
                        escape_val = 0
                    }
                    
                    escape_val *= 10
                    escape_val += c - '0'.code
                
                } else if (c == ';'.code) {
                
                    escape_args.add (escape_val)
                    escape_val = -1
                
                } else if (c == '='.code) {
                
                    escape_args.add (-'='.code)
                    escape_val = -1
                
                } else if (c == '?'.code) {
                
                    escape_args.add (-'?'.code)
                    escape_val = -1
                
                } else if (c >= 0x40 && c <= 0x7E){
                
                    if (escape_val >= 0) {
                        escape_args.add (escape_val)
                    }
                    
                    ansi_term (c)
                    
                    all = true
                    escape_state = 3
                
                }
            
            } else if (c == 10) {       // \n
            
                cur_row++
                cur_col = 0
            
            } else if (c == 13) {       // \r
                cur_col = 0
            } else if (c == 8) {
            
                if (cur_col > 0) {
                
                    cur_col--
                
                    col = row[cur_col]
                    col.txt = " "
                
                }
            
            } else if (c == 0x1B) {     // ESC
                escape_state = 1
            } else {
            
                val sb = StringBuilder ()
                sb.appendCodePoint (c)
                
                col.set (escape_gfx)
                col.txt = sb.toString ()
                
                cur_col++
            
            }
            
            if (cur_col < 0) {
                cur_col = 0
            }
            
            if (cur_row < 0) {
                cur_row = 0
            }
            
            if (cur_col >= 80) {
            
                if (escape_state == 0) {
                
                    cur_row++
                    cur_col = 0
                
                } else {
                    cur_col = 79
                }
            
            }
            
            if (cur_row >= 25) {
            
                if (escape_state == 0) {
                
                    scroll (cur_row - 24)
                    last_row = -1
                
                }
                
                cur_row = 24
            
            }
            
            if (last_row != cur_row) {
            
                all = true
                
                row = rows[cur_row]
                col = row[cur_col]
                
                last_row = cur_row
                last_col = cur_col
            
            } else if (last_col != cur_col) {
            
                col = row[cur_col]
                last_col = cur_col
            
            }
            
            i++
        
        }
        
        activity.draw (all)
    
    }
    
    /* blink the cursor */
    fun draw_cursor(activity: MainActivity) {
    
        if (!cur_show) {
            return
        }
        
        cur_timer++
        
        if (cur_timer == 25) {
        
            val bg = rows[cur_row][cur_col].backgroundColor
            val fg = rows[cur_row][cur_col].foregroundColor
            
            rows[cur_row][cur_col].backgroundColor = fg
            rows[cur_row][cur_col].foregroundColor = bg
            
            activity.draw (false)
            
            rows[cur_row][cur_col].backgroundColor = bg
            rows[cur_row][cur_col].foregroundColor = fg
        
        } else if (cur_timer == 50) {
        
            activity.draw (false)
            cur_timer = 0
        
        }
    
    }

}