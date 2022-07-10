package com.mt.rotarycontrol.customui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mt.rotarycontrol.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RotaryControl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        init(attrs)
    }

    private var numberOfStates: Int = 0
    private var defaultState: Int = 0
    private var currentState: Int = defaultState
    private var actualState: Int = currentState

    private var indicatorWidth: Int = 0
    private var indicatorColor: Int = 0
    private var indicatorRelativeLength: Float = 0.0f

    private var stateMarkersWidth: Int = 0
    private var markersColor: Int = 0
    private var selectedStateMarkerColor: Int = 0

    private var minAngle: Float = 0f
    private var maxAngle: Float = 0f

    private var markersLength: Float = 0f
    private var rotaryTextSize = 14f
    private var tooptipTextSize = 14f
    private var tooptipHeight = 0
    private var tooptipWidth = 0
    private var rotaryType:Int = 0

    private var rotaryDrawable: Int = 0
    private lateinit var roataryBgDrawable: Drawable
    private lateinit var paint: Paint
    private lateinit var textPaint: TextPaint

    private var startPoint = 0
    private var centerPoint = 0
    private var endPoint = 0
    private var currentAngle = 0.0
    private var gestureFloatValue = 0.0

    private var tooltipRadius = 0f
    private var topMostExternalRadius = 0f
    private var externalRadius = 0f
    private var rotaryRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var rotaryStepValues: Array<CharSequence>? = null
    private var rotaryContinuousStopPoint: Array<CharSequence>? = null
    private var rotaryContinuousStopPointUpdated: Array<CharSequence?>? = null

    private var isTooltipErase:Boolean = true

    var listener: OnValueChanged? = null

    interface OnValueChanged {
        fun onValueUpdate(value: String)
    }

    companion object {
        const val ROTARYTYPE_STEP = 0
        const val ROTARYTYPE_CONTINUOUS = 1
    }


    private fun init(attrs: AttributeSet) {
        loadAttributes(attrs)
        initResources()
        initStatus()
        initTouchListener()
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Rotary)
        numberOfStates = typedArray.getInt(R.styleable.Rotary_rotaryNumberOfStates, 0)
        defaultState = typedArray.getInt(R.styleable.Rotary_rotaryDefaultState, 0)

        indicatorWidth =
            typedArray.getDimensionPixelSize(R.styleable.Rotary_rotaryIndicatorWidth, 6)
        indicatorColor = typedArray.getColor(R.styleable.Rotary_rotaryIndicatorColor, Color.BLACK)
        indicatorRelativeLength =
            typedArray.getFloat(R.styleable.Rotary_rotaryIndicatorRelativeLength, 0.35f)

        stateMarkersWidth =
            typedArray.getDimensionPixelSize(R.styleable.Rotary_rotaryMarkersWidth, 5)
        selectedStateMarkerColor =
            typedArray.getColor(R.styleable.Rotary_rotarySelectedMarkerColor, Color.GREEN)

        minAngle = typedArray.getFloat(R.styleable.Rotary_rotaryMinAngle, 0f)
        maxAngle = typedArray.getFloat(R.styleable.Rotary_rotaryMaxAngle, 360f)

        markersColor = typedArray.getColor(R.styleable.Rotary_rotaryMarkersColor, Color.BLACK)
        markersLength = typedArray.getFloat(R.styleable.Rotary_rotaryMarkersLength, 0.11f)
        rotaryDrawable =
            typedArray.getResourceId(R.styleable.Rotary_rotaryDrawable, R.drawable.rotary)
        rotaryTextSize = typedArray.getDimension(R.styleable.Rotary_rotaryTextSize, rotaryTextSize)
        tooptipTextSize =
            typedArray.getDimension(R.styleable.Rotary_tooptipTextSize, tooptipTextSize)
        tooptipWidth =
            typedArray.getDimensionPixelSize(R.styleable.Rotary_tooptipWidth, 0)
        tooptipHeight =
            typedArray.getDimensionPixelSize(R.styleable.Rotary_tooptipHeight, 0)

        rotaryType = rotaryTypeAttrToInt(typedArray.getString(R.styleable.Rotary_rotaryType))
        rotaryStepValues = typedArray.getTextArray(R.styleable.Rotary_rotaryStepValues)
        rotaryContinuousStopPoint =
            typedArray.getTextArray(R.styleable.Rotary_rotaryContinuousStopPoint)

        typedArray.recycle()

        if (rotaryStepValues != null) {
            numberOfStates = rotaryStepValues!!.size
        }

        startPoint = 0
        endPoint = numberOfStates - 1
        if (rotaryContinuousStopPoint != null && rotaryType == ROTARYTYPE_CONTINUOUS) {
            if (rotaryContinuousStopPoint!!.size > 3) {
                throw Exception("Invalid length of ContinuousStopPoint or StepValues in Rotary Type continuous")
            }
            rotaryContinuousStopPointUpdated = arrayOfNulls<CharSequence>(numberOfStates)
            centerPoint = numberOfStates / 2
            rotaryContinuousStopPointUpdated!![startPoint] = rotaryContinuousStopPoint!![0]
            rotaryContinuousStopPointUpdated!![centerPoint] = rotaryContinuousStopPoint!![1]
            rotaryContinuousStopPointUpdated!![endPoint] = rotaryContinuousStopPoint!![2]
        }

    }

    private fun rotaryTypeAttrToInt(s: String?): Int {
        return when (s) {
            "0" -> ROTARYTYPE_STEP
            "1" -> ROTARYTYPE_CONTINUOUS
            else -> ROTARYTYPE_STEP
        }
    }

    private fun initResources() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeCap = Paint.Cap.ROUND
        textPaint = TextPaint()
        textPaint.strokeCap = Paint.Cap.ROUND
        textPaint.textSize = rotaryTextSize
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        if (rotaryDrawable != 0) {
            roataryBgDrawable = ContextCompat.getDrawable(context, rotaryDrawable)!!
        }
    }

    private fun initStatus() {
        currentState = defaultState
        calcActualState()
        currentAngle = calcAngle(currentState)
    }

    private fun calcAngle(position: Int): Double {
        val min = Math.toRadians(minAngle.toDouble())
        val max = Math.toRadians(maxAngle - 0.0001)
        val range = max - min
        if (numberOfStates <= 1) return 0.0
        var singleStepAngle = range / (numberOfStates - 1)
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates
        return normalizeAngle(Math.PI - min - position * singleStepAngle)
    }

    private fun normalizeAngle(angle: Double): Double {
        var angl = angle
        while (angl < 0) angl += Math.PI * 2
        while (angl >= Math.PI * 2) angl -= Math.PI * 2
        return angl
    }

    private fun calcActualState() {
        actualState = currentState % numberOfStates
        if (actualState < 0) actualState += numberOfStates
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListener() {
        setOnTouchListener { _, motionEvent ->
            val action = motionEvent.action
            val x = motionEvent.x.toInt()
            val y = motionEvent.y.toInt()
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    isTooltipErase = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val angle = atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                    setValueByAngle(angle)
                }
                MotionEvent.ACTION_UP -> {
                    isTooltipErase = true
                }
            }
            true
        }
    }


    private fun setValueByAngle(angle: Double) {
        var angl = angle
        if (numberOfStates <= 1) {
            return
        }
        gestureFloatValue = angl
        var min = Math.toRadians(minAngle.toDouble())
        var max = Math.toRadians(maxAngle - 0.0001)
        val range = max - min
        var singleStepAngle = range / numberOfStates
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates
        min = normalizeAngle(min).toFloat().toDouble()
        while (min > max) max += 2 * Math.PI // both min and max are positive and in the correct order.
        angl = normalizeAngle(angl + Math.PI / 2)
        while (angl < min) angl += 2 * Math.PI // set angle after min angle
        if (angl > max) { // if angle is out of range because the range is limited set to the closer limit
            angl = if (angl - max > min - angl + Math.PI * 2) min else max
        }
        currentState = ((angl - min) / singleStepAngle).toInt() // calculate value
        calcActualState()
        takeEffect()
    }


    private fun takeEffect() {
        currentAngle = calcAngle(actualState)
        postInvalidate()
    }

    private fun tooltipText(): String {
        return if (rotaryType == ROTARYTYPE_STEP) {
            rotaryStepValues!![actualState].toString()
        } else {
            String.format("%.02f", gestureFloatValue)
        }
    }

    private fun paintRotary(canvas: Canvas) {
        if (rotaryDrawable != 0) {
            roataryBgDrawable.setBounds(
                (centerX - rotaryRadius).toInt(),
                (centerY - rotaryRadius).toInt(),
                (centerX + rotaryRadius).toInt(),
                (centerY + rotaryRadius).toInt()
            )
            roataryBgDrawable.draw(canvas)
        } else {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, rotaryRadius, paint)
        }
    }

    private fun paintMarkers(canvas: Canvas) {
        for (state in 0 until numberOfStates) {
            val angle = calcAngle(state)
            val selected: Boolean = state == actualState || state <= actualState
            textPaint.color = if (selected) selectedStateMarkerColor else markersColor

            if (rotaryType == ROTARYTYPE_STEP) {
                // Marker
                if (state == startPoint || state == endPoint) {
                    paint.strokeWidth = stateMarkersWidth.toFloat()
                    paint.color = if (selected) selectedStateMarkerColor else markersColor

                    val startX =
                        centerX + (externalRadius * (1 - markersLength) * sin(angle)).toFloat()
                    val startY =
                        centerY + (externalRadius * (1 - markersLength) * cos(angle)).toFloat()

                    val endX = centerX + (externalRadius * sin(angle)).toFloat()
                    val endY = centerY + (externalRadius * cos(angle)).toFloat()

                    canvas.drawLine(startX, startY, endX, endY, paint)
                }

                // Label
                if(rotaryStepValues != null) {
                    val topMostX = centerX + (topMostExternalRadius * sin(angle)).toFloat()
                    val topMostY = centerY + (topMostExternalRadius * cos(angle)).toFloat()
                    val text = rotaryStepValues!![state].toString()
                    val width = textPaint.measureText(text).toInt()
                    if (centerX < topMostX) {
                        canvas.drawText(text, topMostX, topMostY, textPaint)
                    } else {
                        canvas.drawText(text, topMostX - width, topMostY, textPaint)
                    }
                }
            } // End of ROTARYTYPE_STEP
            else if (rotaryType == ROTARYTYPE_CONTINUOUS) {
                // Marker
                if (state == startPoint || state == endPoint) {
                    paint.strokeWidth = stateMarkersWidth.toFloat()
                    paint.color = if (selected) selectedStateMarkerColor else markersColor
                    val startX =
                        centerX + (externalRadius * (1 - markersLength) * sin(angle)).toFloat()
                    val startY =
                        centerY + (externalRadius * (1 - markersLength) * cos(angle)).toFloat()
                    val endX = centerX + (externalRadius * sin(angle)).toFloat()
                    val endY = centerY + (externalRadius * cos(angle)).toFloat()
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
                // Label
                if (state == startPoint || state == centerPoint || state == endPoint) {
                    val topMostX = centerX + (topMostExternalRadius * sin(angle)).toFloat()
                    val topMostY = centerY + (topMostExternalRadius * cos(angle)).toFloat()
                    val text = rotaryContinuousStopPointUpdated!![state].toString()
                    val width = textPaint.measureText(text).toInt()
                    if (centerX < topMostX) {
                        canvas.drawText(text, topMostX, topMostY + width, textPaint)
                    } else {
                        canvas.drawText(text, topMostX - width, topMostY, textPaint)
                    }
                }
            }
        }
    }


    private fun paintIndicator(canvas: Canvas) {
        if (indicatorWidth == 0) return
        if (indicatorRelativeLength == 0.0f) return
        paint.color = indicatorColor
        paint.strokeWidth = indicatorWidth.toFloat()
        val startX =
            centerX + (rotaryRadius * (1 - indicatorRelativeLength) * sin(currentAngle)).toFloat()
        val startY =
            centerY + (rotaryRadius * (1 - indicatorRelativeLength) * cos(currentAngle)).toFloat()
        val endX = centerX + (rotaryRadius * sin(currentAngle)).toFloat()
        val endY = centerY + (rotaryRadius * cos(currentAngle)).toFloat()
        canvas.drawLine(startX, startY, endX, endY, paint)
    }



    private fun showTooltip(canvas: Canvas) {
        if(isTooltipErase) {
            val indicatorRelativeLength = 0.0f

            val currentAngle = calcAngle(numberOfStates / 4).toInt()

            val startX =
                centerX + (tooltipRadius * (1 - indicatorRelativeLength) * sin(currentAngle.toDouble())).toFloat()
            val startY =
                centerY + (tooltipRadius * (1 - indicatorRelativeLength) * cos(currentAngle.toDouble())).toFloat()

            canvas.translate((startX - tooptipWidth * 0.7).toFloat(), startY)
        }
        else {
            val layout = LinearLayout(context)
            val params = LinearLayout.LayoutParams(tooptipWidth, tooptipHeight)

            val textView = TextView(context)
            textView.layoutParams = params

            textView.setPadding(20, 10, 10, 10)
            textView.setBackgroundResource(R.drawable.popup)

            textView.visibility = VISIBLE
            val value = tooltipText()
            textView.text = value
            textView.textSize = tooptipTextSize.toInt().toFloat()
            textView.gravity = Gravity.CENTER
            layout.addView(textView)

            layout.measure(canvas.width, canvas.height)
            layout.layout(0, 0, canvas.width, canvas.height)

            val indicatorRelativeLength = 0.0f

            val currentAngle = calcAngle(numberOfStates / 4).toInt()

            val startX =
                centerX + (tooltipRadius * (1 - indicatorRelativeLength) * sin(currentAngle.toDouble())).toFloat()
            val startY =
                centerY + (tooltipRadius * (1 - indicatorRelativeLength) * cos(currentAngle.toDouble())).toFloat()

            canvas.translate((startX - tooptipWidth * 0.7).toFloat(), startY)
            layout.draw(canvas)

            // Callback to send the value
            listener?.onValueUpdate(value)
        }

    }

    ////////////////////////////////////////////////////

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // It draws Image
        paintRotary(canvas)
        // It draws Outline dots or indicator
        paintMarkers(canvas)
        // It draws Handle kind of indicator
        paintIndicator(canvas)
        // It draws Tooltip
        showTooltip(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var wms: Int = widthMeasureSpec
        var hms: Int = heightMeasureSpec

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val r = Resources.getSystem()

        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            widthSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, r.displayMetrics)
                    .toInt()
            wms = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
        }

        if (heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST) {
            heightSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, r.displayMetrics)
                    .toInt()
            hms = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        }
        super.onMeasure(wms, hms)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = width
        val height = height
        tooltipRadius = width.coerceAtLeast(height) * 0.5f
        topMostExternalRadius = tooltipRadius * 0.5f
        externalRadius = topMostExternalRadius * 0.8f
        rotaryRadius = externalRadius * 0.8f
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
    }





}