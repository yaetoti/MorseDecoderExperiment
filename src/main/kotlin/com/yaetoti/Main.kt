package com.yaetoti

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Dictionary
import javax.sound.sampled.AudioSystem
import kotlin.math.abs

class AudioData(val sampleRate: Float, val samples: FloatArray, val amplitude: Short, var length: Float) {
}

fun readWavSamples(file: File): AudioData {
  AudioSystem.getAudioInputStream(file).use {
    val format = it.format
    require(format.sampleSizeInBits == 16 && format.channels == 1) {
      "IDI NAHUI"
    }
    val bytes = it.readAllBytes()
    val samples = ShortArray(bytes.size / 2)
    ByteBuffer.wrap(bytes)
      .order(if (format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
      .asShortBuffer()
      .get(samples)

    // Preprocess
    val maxAmplitude = samples.maxOf { abs(it.toInt()).toShort() }
    val normalized = samples.map { Math.clamp(it.toFloat() / maxAmplitude.toFloat(), -1.0f, 1.0f) }.toFloatArray()

    return AudioData(format.sampleRate, normalized, maxAmplitude, samples.size / format.sampleRate)
  }
}

fun handleAudio(data: AudioData): String {
  var sb = StringBuilder()

  val dotTime = 0.04f
  val dashTime = 0.15f

  val symbolTime = 0.18f
  val spaceTime = 0.40f

  val attackTime = 0.0005f
  val attackBuffer = ArrayDeque<Float>()
  val dt = 1.0f / data.sampleRate

  var silenceTime = 0.0f
  var beepTime = 0.0f

  var isBeep = false

  for (sample in data.samples) {
    // Update buffer
    if (attackBuffer.size >= (attackTime * data.sampleRate).toInt()) {
      attackBuffer.removeLastOrNull()
    }

    attackBuffer.addFirst(sample)

    // Calculate max amplitude
    val maxLocalAmplitude = attackBuffer.maxOf { abs(it) }

    val onPreSwitch = fun(isBeep: Boolean) {
      if (isBeep) {
        if (beepTime > dashTime) {
          sb.append('-')
          return
        }
        if (beepTime > dotTime) {
          sb.append('.')
          return
        }
      } else {
        if (silenceTime > spaceTime) {
          sb.append(' ')
          sb.append(' ')
          return
        }

        if (silenceTime > symbolTime) {
          sb.append(' ')
          return
        }
      }
    }

    // Switch and time accumulation
    if (maxLocalAmplitude > 0.5f) {
      if (!isBeep) {
        onPreSwitch(isBeep)

        isBeep = true
        silenceTime = 0.0f
        beepTime = 0.0f
      }

      beepTime += dt
    } else {
      if (isBeep) {
        onPreSwitch(isBeep)

        isBeep = false
        silenceTime = 0.0f
        beepTime = 0.0f
      }

      silenceTime += dt
    }
  }

  return sb.toString()
}

fun parse(s: String): String {
  val words = mapOf<String, Char>(
    ".-" to 'а',
    "-..." to 'б',
    ".--" to 'в',
    "--." to 'г',
    "-.." to 'д',
    "." to 'е',
    "..-.." to 'є',
    "...-" to 'ж',
    "--.." to 'з',
    "-.--" to 'и',
    ".." to 'і',
    ".---" to 'й',
    "-.-" to 'к',
    ".-.." to 'л',
    "--" to 'м',
    "-." to 'н',
    "---" to 'о',
    ".--." to 'п',
    ".-." to 'р',
    "..." to 'с',
    "-" to 'т',
    "..-" to 'у',
    "..-." to 'ф',
    "...." to 'х',
    "-.-." to 'ц',
    "---." to 'ч',
    "----" to 'ш',
    "--.-" to 'щ',
    "-..-" to 'ь',
    "..--" to 'ю',
    ".-.-" to 'я',

    "-----" to '0',
    ".----" to '1',
    "..---" to '2',
    "...--" to '3',
    "....-" to '4',
    "....." to '5',
    "-...." to '6',
    "--..." to '7',
    "---.." to '8',
    "----." to '9',

    ".-.-.-" to '.',
    "--..--" to ',',
    "..--.." to '?',
    ".----." to '\'',
    "-.-.--" to '!',
    "-..-." to '/',
    "-.--." to '(',
    "-.--.-" to ')',
    ".-..." to '&',
    "---..." to ':',
    "-.-.-." to ';',
    "-...-" to '=',
    ".-.-." to '+',
    "-....-" to '-',
    "..--.-" to '_',
    ".-..-." to '"',
    "...-..-" to '$',
    ".--.-." to '@'
  )

  val sb = StringBuffer()
  var inWord = false
  var wordStart = 0

  var pos = 0
  while (pos < s.length) {
    val c = s[pos]
    if (c == ' ') {
      inWord = false

      // Word end
      var word = s.substring(wordStart, pos)
      if (words.containsKey(word)) {
        val res = words[word]
        sb.append(res)
      }

      // space
      if (pos + 1 < s.length && s[pos + 1] == ' ') {
        sb.append(' ')
        pos += 2
        continue
      }
    } else {
      if (!inWord) {
        wordStart = pos
      }

      inWord = true
    }

    ++pos
  }

  return sb.toString()
}

fun main() {
  val filename = "jiggleWiggle.wav"
  val data = readWavSamples(File(filename))
  val code = handleAudio(data)
  val result = parse(code)
  println(code)
  println(result)
}
