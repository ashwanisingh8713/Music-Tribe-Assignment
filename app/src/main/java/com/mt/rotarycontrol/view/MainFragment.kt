package com.mt.rotarycontrol.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.mt.rotarycontrol.databinding.FragmentMainBinding
import com.mt.rotarycontrol.customui.RotaryControl
import com.mt.rotarycontrol.viewmodel.MainViewModel


class MainFragment: Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        registerObserverForRotaryValue()

        // It listens Rotary Control value change
        binding.rotaryStep.listener = object :RotaryControl.OnValueChanged {
            override fun onValueUpdate(value: String) {
                viewModel.setRotaryStepState(value)
            }
        }

        // It listens Rotary Control value change
        binding.rotaryContinuous.listener = object :RotaryControl.OnValueChanged {
            override fun onValueUpdate(value: String) {
                viewModel.setRotaryContinuousState(value)
            }
        }

    }

    /**
     * Observer is waiting for viewModel to update our UI
     */
    private fun registerObserverForRotaryValue() {
        // Observer for Rotary Controller Step
        viewModel.liveDataRotaryStep.observe(viewLifecycleOwner) { updatedText ->
            binding.rotaryStepValue.text = updatedText
        }

        // Observer for Rotary Controller Continuous
        viewModel.liveDataRotaryContinuous.observe(viewLifecycleOwner) { updatedText ->
            binding.rotaryContinuousValue.text = updatedText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}