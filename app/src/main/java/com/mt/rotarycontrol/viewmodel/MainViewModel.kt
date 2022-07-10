package com.mt.rotarycontrol.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MainViewModel: ViewModel() {

    val liveDataRotaryStep = MutableLiveData<String>()
    val liveDataRotaryContinuous = MutableLiveData<String>()


    fun setRotaryStepState(state: String) {
        liveDataRotaryStep.postValue(state)
    }

    fun setRotaryContinuousState(state: String) {
        liveDataRotaryContinuous.postValue(state)
    }


}