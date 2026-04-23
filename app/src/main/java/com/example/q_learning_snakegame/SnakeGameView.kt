package com.example.q_learning_snakegame

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SnakeGameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    var snakeBody: List<Position> = emptyList()
    var foodPosition: Position? = null
    private val paint = Paint()

    private var appleBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.apple)
    private var grassBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.grass)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Arka plan
        if (grassBitmap != null) {
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(grassBitmap!!, width, height, true),
                0f, 0f, paint
            )
        } else {
            canvas.drawColor(Color.GREEN)
        }

        val gridSize = width / 20f

        // Yem
        foodPosition?.let {
            val x = it.x * gridSize
            val y = it.y * gridSize
            appleBitmap?.let { bmp ->
                val apple = Bitmap.createScaledBitmap(
                    bmp, gridSize.toInt(), gridSize.toInt(), true
                )
                canvas.drawBitmap(apple, x, y, paint)
            }
        }

        // Yılan
        snakeBody.forEachIndexed { index, pos ->
            val left = pos.x * gridSize
            val top = pos.y * gridSize
            paint.color = if (index == 0) Color.rgb(62,39,35) else Color.rgb(121,85,72)
            canvas.drawRoundRect(
                RectF(left+2, top+2, left+gridSize-2, top+gridSize-2),
                15f, 15f, paint
            )
        }
    }
}
