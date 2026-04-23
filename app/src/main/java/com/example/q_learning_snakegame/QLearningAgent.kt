package com.example.q_learning_snakegame

import kotlin.math.max
import kotlin.random.Random

class QLearningAgent(
    private val alpha: Double = 0.1,   // öğrenme oranı
    private val gamma: Double = 0.9,   // gelecek ödül indirgeme
    private var epsilon: Double = 1.0, // keşif oranı
    private val epsilonMin: Double = 0.05,
    private val epsilonDecay: Double = 0.995
) {
    // Her state için 3 aksiyonun Q değeri: [Left, Straight, Right]
    private val qTable: MutableMap<String, DoubleArray> = HashMap()
//bulunan durum ilk kez görüldüyse state olustur
    private fun ensureState(state: String): DoubleArray {
        return qTable.getOrPut(state) { DoubleArray(3) { 0.0 } }
    }


    //ajan hang, aksiyonu seçmeli ? karar?
    fun getAction(state: String): Int {
        val qValues = ensureState(state)

        // epsilon-greedy: bazen rastgele seç (explore)
        if (Random.nextDouble() < epsilon) {
            return Random.nextInt(3)
        }

        // exploit: en iyi Q aksiyonu
        var bestA = 0
        var bestQ = qValues[0]
        for (a in 1..2) {
            if (qValues[a] > bestQ) {
                bestQ = qValues[a]
                bestA = a
            }
        }
        return bestA
    }

    fun train(state: String, action: Int, reward: Double, nextState: String) {
        val qS = ensureState(state)
        val qNext = ensureState(nextState)

        val maxNext = max(qNext[0], max(qNext[1], qNext[2]))
        val oldQ = qS[action]
        val target = reward + gamma * maxNext

        qS[action] = oldQ + alpha * (target - oldQ)

        // epsilon azalt (zamanla daha az rastgele)
        if (epsilon > epsilonMin) {
            epsilon *= epsilonDecay
            if (epsilon < epsilonMin) epsilon = epsilonMin
        }
    }

    fun resetLearning() {
        qTable.clear()
        epsilon = 1.0
    }

    fun getEpsilon(): Double = epsilon
}
