package com.example.mediaplayerdemo

const val ACTION_PLAY = "ACTION_PLAY"
const val ACTION_PREV = "ACTION_PREV"
const val ACTION_NEXT = "ACTION_NEXT"

interface ActionInterface {
    fun playPause()
    fun playNext()
    fun playPrev()
}