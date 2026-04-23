package com.example.q_learning_snakegame

import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var btnPause: Button
    private lateinit var btnToggleAI: Button
    private lateinit var btnUp: Button
    private lateinit var btnDown: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button

    private lateinit var prefs: SharedPreferences

    // Oyun verileri
    private val snakeBody = ArrayList<Position>()
    private var foodPosition = Position(5, 5)

    // 0=Up,1=Right,2=Down,3=Left
    private var direction = 1
    private var nextDirection = 1

    private var boardWidth = 20
    private var boardHeight = 20

    private var score = 0
    private var gameSpeed = 120L
    private var isPaused = false
    private var gameOver = false
    private var isAiMode = false

    private val handler = Handler(Looper.getMainLooper())

    // Q-Learning Agent
    private val agent = QLearningAgent()

    private var prevManhattanDist = 0

    // -------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("scores", MODE_PRIVATE)

        gameView = findViewById(R.id.gameView)
        scoreText = findViewById(R.id.scoreText)
        btnPause = findViewById(R.id.btnPause)
        btnToggleAI = findViewById(R.id.btnToggleAI)
        btnUp = findViewById(R.id.btnUp)
        btnDown = findViewById(R.id.btnDown)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)

        // Board yüksekliği GameView’e göre hesaplanır
        gameView.post {
            val cellSize = gameView.width / boardWidth.toFloat()
            boardHeight = (gameView.height / cellSize).toInt() - 1
            resetGame()
            runGameLoop()
        }

        setupControls()
        listOf(btnUp, btnDown, btnLeft, btnRight).forEach { addPressEffect(it) }
    }

    // -------------------------------------------------
    // BUTON BASMA EFEKTİ
    private fun addPressEffect(v: View) {
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN ->
                    v.background.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.background.clearColorFilter()
            }
            false
        }
    }

    // -------------------------------------------------
    // KONTROLLER--insan modu
    private fun setupControls() {

        // İnsan modu yön tuşları
        btnUp.setOnClickListener { if (!isAiMode && !isPaused && !gameOver) setNextDir(0) }
        btnRight.setOnClickListener { if (!isAiMode && !isPaused && !gameOver) setNextDir(1) }
        btnDown.setOnClickListener { if (!isAiMode && !isPaused && !gameOver) setNextDir(2) }
        btnLeft.setOnClickListener { if (!isAiMode && !isPaused && !gameOver) setNextDir(3) }

        // AI aç/kapa
        btnToggleAI.setOnClickListener {
            isAiMode = !isAiMode
            btnToggleAI.text = if (isAiMode) "Y. ZEKA" else "İNSAN"
            gameSpeed = if (isAiMode) 40L else 120L
            resetGame()
        }

        // PAUSE / PLAY
        btnPause.setOnClickListener {
            if (!gameOver) {
                isPaused = !isPaused

                if (isPaused) {
                    // DURDURULDU
                    btnPause.setBackgroundResource(R.drawable.btn_play_wood)
                    handler.removeCallbacks(gameRunnable)
                } else {
                    // DEVAM
                    btnPause.setBackgroundResource(R.drawable.btn_pause_wood)
                    runGameLoop()
                }
            }
        }
    }

    private fun setNextDir(newDir: Int) {
        val isOpp = (direction == 0 && newDir == 2) ||
                (direction == 2 && newDir == 0) ||
                (direction == 1 && newDir == 3) ||
                (direction == 3 && newDir == 1)
        if (!isOpp) nextDirection = newDir
    }

    // -------------------------------------------------
    // GAME LOOP
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && !gameOver) {
                step()

                gameView.snakeBody = snakeBody
                gameView.foodPosition = foodPosition
                gameView.invalidate()

                handler.postDelayed(this, gameSpeed)
            }
        }
    }

    private fun runGameLoop() {
        handler.removeCallbacks(gameRunnable)
        handler.post(gameRunnable)
    }

    // -------------------------------------------------
    // OYUN ADIMI
    private fun step() {
        if (snakeBody.isEmpty()) return

        if (isAiMode) {
            val state = getState()//Qlearning state
            val action = agent.getAction(state)//policy qtable a bakılır ve en iyi veya random sonuç
            applyRelativeAction(action)

            val newHead = computeNextHead(direction)//bi sonraki yılan kafasını hesaplar
            val (reward, nextState) = processMoveAI(newHead)//çarpışma ve ödül

            agent.train(state, action, reward, nextState)//güncelleme  Q(s,a)←Q(s,a)+α[r+γmaxQ(s′,a′)−Q(s,a)]
        } else { //insan
            direction = nextDirection
            val newHead = computeNextHead(direction)
            processMoveHuman(newHead)
        }

        updateScoreText() //uı degistir
    }

    // -------------------------------------------------
    // AI ACTION
    private fun applyRelativeAction(action: Int) {
        direction = when (action) {
            0 -> (direction + 3) % 4  //sag
            2 -> (direction + 1) % 4 //sol
            else -> direction
        } //şu an gittiğin yöne göre sola / düz / sağa git
    }

    private fun computeNextHead(dir: Int): Position {
        val h = snakeBody[0]//1 hamle ileri git
        return when (dir) {
            0 -> Position(h.x, h.y - 1)
            1 -> Position(h.x + 1, h.y)
            2 -> Position(h.x, h.y + 1)
            else -> Position(h.x - 1, h.y)
        }
    }

    //Algıla (state)
    //   ↓
    //Karar ver (action)
    //   ↓
    //Uygula (movement)
    //   ↓
    //Sonucu değerlendir (reward)
    //   ↓
    //Öğren (Q update)


    // -------------------------------------------------
    // AI MOVE + REWARD
    private fun processMoveAI(newHead: Position): Pair<Double, String> {
//noldu çarpıştı mı yem mi yedi oyun mu bitti
        if (checkCollision(newHead)) {
            gameOver = true
            saveHighScore(score)

            handler.postDelayed({
                resetGame()
                runGameLoop()
            }, 600)

            return Pair(-100.0, "CRASH")
        }

        snakeBody.add(0, newHead)

        var reward = -0.2  // boş dolasmısnı engeller

        if (newHead == foodPosition) {
            score++
            reward += 50.0
            spawnFood()
        } else {
            snakeBody.removeAt(snakeBody.size - 1)
        }

        val newDist = manhattan(newHead, foodPosition)
        reward += if (newDist < prevManhattanDist) 1.5 else -1.0
        prevManhattanDist = newDist
        //yeni kafa yeme yakınsa uzaksa , yakın olması iyi +1.5

        return Pair(reward, getState())
    }

    // -------------------------------------------------
    // HUMAN MOVE
    private fun processMoveHuman(newHead: Position) {
        if (checkCollision(newHead)) {
            gameOver = true
            saveHighScore(score)

            handler.postDelayed({
                resetGame()
                runGameLoop()
            }, 600)
            return
        }

        snakeBody.add(0, newHead)
        if (newHead == foodPosition) {
            score++
            spawnFood()
        } else {
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    // -------------------------------------------------
    // STATE
    //ajan çevreyi algılıyor
    private fun getState(): String {
        val h = snakeBody[0]
//önümde sagımda solumda çarpısma var mı
        val dangerF = if (willCollide(direction)) 1 else 0
        val dangerL = if (willCollide((direction + 3) % 4)) 1 else 0
        val dangerR = if (willCollide((direction + 1) % 4)) 1 else 0

        val foodDir = when {
            foodPosition.x > h.x -> "R"
            foodPosition.x < h.x -> "L"
            foodPosition.y > h.y -> "D"
            else -> "U"
        }

        return "${foodDir}_${dangerF}${dangerL}${dangerR}_D$direction"
    }

    private fun willCollide(dir: Int): Boolean {//bu yöne gitsem ölür müyüm
        val h = snakeBody[0]
        val p = when (dir) {
            0 -> Position(h.x, h.y - 1)
            1 -> Position(h.x + 1, h.y)
            2 -> Position(h.x, h.y + 1)
            else -> Position(h.x - 1, h.y)
        }
        return checkCollision(p)
    }

    // -------------------------------------------------
    // YARDIMCILAR
    private fun manhattan(a: Position, b: Position): Int =
        abs(a.x - b.x) + abs(a.y - b.y)

    private fun checkCollision(p: Position): Boolean {
        if (p.x !in 0 until boardWidth || p.y !in 0 until boardHeight) return true
        //duvar çarpısma
        return snakeBody.drop(1).any { it == p }//kendine çarpma
    }// yılan ve elmaya uzaklık manhattan mesafesi

    private fun spawnFood() {
        do {
            foodPosition = Position(
                Random.nextInt(boardWidth),
                Random.nextInt(boardHeight)
            )
        } while (snakeBody.contains(foodPosition))
    }

    // -------------------------------------------------
    // RESET
    private fun resetGame() {
        snakeBody.clear()
        snakeBody.add(Position(10, 10))
        snakeBody.add(Position(10, 11))

        direction = 1
        nextDirection = 1
        score = 0
        gameOver = false
        isPaused = false

        btnPause.setBackgroundResource(R.drawable.btn_pause_wood)

        spawnFood()
        prevManhattanDist = manhattan(snakeBody[0], foodPosition)

        updateScoreText()
    }

    // -------------------------------------------------
    // SKOR
    private fun updateScoreText() {
        val best = prefs.getInt("highScore", 0)
        val mode = if (isAiMode) "Y. ZEKA" else "İNSAN"
        scoreText.text = "Skor: $score | En Yüksek: $best | $mode"
    }

    private fun saveHighScore(s: Int) {
        val best = prefs.getInt("highScore", 0)
        if (s > best) prefs.edit().putInt("highScore", s).apply()
    }
}
