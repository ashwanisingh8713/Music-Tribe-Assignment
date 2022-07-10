package com.mt.rotarycontrol.ui

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
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.mt.rotarycontrol.R
import kotlin.properties.Delegates

class RotaryControl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    init {
        init(attrs);
    }

    private var ctx: Context? = null

//    private var numberOfStates = 6
//    private var numberOfStates: Int by Delegates.notNull<Int>()
    private var numberOfStates: Int? = null
    private var defaultState = 0
    private var currentState = defaultState // can be negative and override expected limits
    private var actualState = currentState // currentState, modded to the expected limits

    private var indicatorWidth = 6
    private var indicatorColor = Color.BLACK
    private var indicatorRelativeLength = 0.35f

    private var stateMarkersWidth = 5
    private var markersColor = Color.BLACK
    private var selectedStateMarkerColor = Color.GREEN

    private var minAngle = 0f
    private var maxAngle = 360f

    private var markersLength = 0.11f
    private var rotaryTextSize = 14f
    private var tooptipTextSize = 14f
    private var tooptipHeight = 0
    private var tooptipWidth = 0
    private var rotaryType = 0

    private var rotaryDrawable = 0
    private lateinit var roataryBgDrawable: Drawable
    private lateinit var paint: Paint
    private lateinit var textPaint: TextPaint

    private var startPoint = 0
    private var centerPoint = 0
    private var endPoint = 0
    private var currentAngle = 0.0
    private var gestureFloatValue = 0.0

    val ROTARYTYPE_STEP = 0
    val ROTARYTYPE_CONTINUOUS = 1

    private var tooltipRadius = 0f
    private var topMostExternalRadius = 0f
    private var externalRadius = 0f
    private var rotaryRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var rotaryStepValues: Array<CharSequence>? = null
    private var rotaryContinuousStopPoint: Array<CharSequence>? = null
    private var rotaryContinuousStopPointUpdated: Array<CharSequence?>? = null


    // initialize
    @Throws(Exception::class)
    fun init(attrs: AttributeSet) {
        ctx = context

        loadAttributes(attrs)
        initResources()
        initStatus()
        initListeners()

    }

    @Throws(Exception::class)
    fun loadAttributes(attrs: AttributeSet) {
        if (attrs == null) return

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Rotary)

        numberOfStates = typedArray.getInt(R.styleable.Rotary_rotaryNumberOfStates, 0)
        defaultState = typedArray.getInt(R.styleable.Rotary_rotaryDefaultState, defaultState)

        indicatorWidth = typedArray.getDimensionPixelSize(R.styleable.Rotary_rotaryIndicatorWidth, indicatorWidth)
        indicatorColor = typedArray.getColor(R.styleable.Rotary_rotaryIndicatorColor, indicatorColor)
        indicatorRelativeLength = typedArray.getFloat(R.styleable.Rotary_rotaryIndicatorRelativeLength, indicatorRelativeLength)

        stateMarkersWidth = typedArray.getDimensionPixelSize(R.styleable.Rotary_rotaryMarkersWidth, stateMarkersWidth)
        selectedStateMarkerColor = typedArray.getColor(R.styleable.Rotary_rotarySelectedMarkerColor, selectedStateMarkerColor)

        minAngle = typedArray.getFloat(R.styleable.Rotary_rotaryMinAngle, minAngle)
        maxAngle = typedArray.getFloat(R.styleable.Rotary_rotaryMaxAngle, maxAngle)

        markersColor = typedArray.getColor(R.styleable.Rotary_rotaryMarkersColor, markersColor)
        markersLength = typedArray.getFloat(R.styleable.Rotary_rotaryMarkersLength, markersLength)
        rotaryDrawable = typedArray.getResourceId(R.styleable.Rotary_rotaryDrawable, rotaryDrawable)
        rotaryTextSize = typedArray.getDimension(R.styleable.Rotary_rotaryTextSize, rotaryTextSize)
        tooptipTextSize = typedArray.getDimension(R.styleable.Rotary_tooptipTextSize, tooptipTextSize)
        tooptipWidth = typedArray.getDimensionPixelSize(R.styleable.Rotary_tooptipWidth, tooptipWidth)
        tooptipHeight = typedArray.getDimensionPixelSize(R.styleable.Rotary_tooptipHeight, tooptipHeight)

        rotaryType = rotaryTypeAttrToInt(typedArray.getString(R.styleable.Rotary_rotaryType))
        rotaryStepValues = typedArray.getTextArray(R.styleable.Rotary_rotaryStepValues)
        rotaryContinuousStopPoint = typedArray.getTextArray(R.styleable.Rotary_rotaryContinuousStopPoint)

        typedArray.recycle()

        if (rotaryStepValues != null) {
            numberOfStates = rotaryStepValues!!.size
        }
        Log.i("Ashwani", "loadAttributes() :: numberOfStates :: "+numberOfStates)
        startPoint = 0
        endPoint = numberOfStates!! - 1

        if (rotaryContinuousStopPoint !=null && rotaryType == ROTARYTYPE_CONTINUOUS) {
            if (rotaryContinuousStopPoint!!.size > 3) {
                throw java.lang.Exception("Invalid length of ContinuousStopPoint or StepValues in Rotary Type continuous")
            }
            rotaryContinuousStopPointUpdated = arrayOfNulls<CharSequence>(4)

            centerPoint = numberOfStates!! / 2
            rotaryContinuousStopPointUpdated!![startPoint] = rotaryContinuousStopPoint!![0]
            rotaryContinuousStopPointUpdated!![centerPoint] = rotaryContinuousStopPoint!![1]
            rotaryContinuousStopPointUpdated!![endPoint] = rotaryContinuousStopPoint!![2]
        }

    }

    private fun rotaryTypeAttrToInt(s: String?): Int {
        if (s == null) return ROTARYTYPE_STEP
        if (s == "0") return ROTARYTYPE_STEP
        else if (s == "1") return ROTARYTYPE_CONTINUOUS
        else return ROTARYTYPE_STEP
    }

    private fun initResources() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeCap = Paint.Cap.ROUND
        textPaint = TextPaint()
        textPaint.strokeCap = Paint.Cap.ROUND
        textPaint.textSize = rotaryTextSize
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        if (rotaryDrawable != 0) {
            roataryBgDrawable = ContextCompat.getDrawable(ctx!!, rotaryDrawable)!!
        }
    }

    fun initStatus() {
        currentState = defaultState
        calcActualState()
        currentAngle = calcAngle(currentState)
    }

    private fun calcAngle(position: Int): Double {
        val min = Math.toRadians(minAngle.toDouble())
        val max = Math.toRadians(maxAngle - 0.0001)
        val range = max - min
        if (numberOfStates!! <= 1) return 0.0
        var singleStepAngle = range / (numberOfStates!! - 1)
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates!!
        return normalizeAngle(Math.PI - min - position * singleStepAngle)
    }

    private fun normalizeAngle(angle: Double): Double {
        var angle = angle
        while (angle < 0) angle += Math.PI * 2
        while (angle >= Math.PI * 2) angle -= Math.PI * 2
        return angle
    }

    private fun calcActualState() {
        actualState = currentState % numberOfStates!!
        if (actualState < 0) actualState += numberOfStates!!
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initListeners() {
        setOnTouchListener { view, motionEvent ->
            val action = motionEvent.action
            val x = motionEvent.x.toInt()
            val y = motionEvent.y.toInt()
            if (action == MotionEvent.ACTION_DOWN) {

            } else if (action == MotionEvent.ACTION_MOVE) {
                val angle = Math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                Log.i("Ashwani", "Touch Angle :: $angle")
                setValueByAngle(angle)
            } else if (action == MotionEvent.ACTION_UP) {
            }
            true
        }
    }

    private fun setValueByAngle(angle: Double) {  // sets the value of the knob given an angle instead of a state
        var angle = angle
        if (numberOfStates!! <= 1) {
            return
        }
        gestureFloatValue = angle
        var min = Math.toRadians(minAngle.toDouble())
        var max = Math.toRadians(maxAngle - 0.0001)
        val range = max - min
        var singleStepAngle = range / numberOfStates!!
        if (Math.PI * 2 - range < singleStepAngle) singleStepAngle = range / numberOfStates!!
        min = normalizeAngle(min).toFloat().toDouble()
        while (min > max) max += 2 * Math.PI // both min and max are positive and in the correct order.
        angle = normalizeAngle(angle + Math.PI / 2)
        while (angle < min) angle += 2 * Math.PI // set angle after minangle
        if (angle > max) { // if angle is out of range because the range is limited set to the closer limit
            angle = if (angle - max > min - angle + Math.PI * 2) min else max
        }
        currentState = ((angle - min) / singleStepAngle).toInt() // calculate value
        calcActualState()
//        if (listener != null) listener.onState(actualState)
        takeEffect()
    }


    private fun takeEffect() {
        currentAngle = calcAngle(actualState)
        postInvalidate()
    }

    fun tooltipText(): String {
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
        selectedStateMarkerColor = Color.BLUE
        markersColor = Color.YELLOW

        Log.i("Ashwani", "paintMarkers() :: numberOfStates :: "+numberOfStates)

        for (i in 0 until numberOfStates!!) {
            var selected = false
            val angle = calcAngle(i)
            selected = i == actualState || i <= actualState
            textPaint.color = if (selected) selectedStateMarkerColor else markersColor

            if (rotaryType == ROTARYTYPE_STEP) {
                // Marker
                if (i == startPoint || i == endPoint) {
                    paint.strokeWidth = stateMarkersWidth.toFloat()
                    paint.color = if (selected) selectedStateMarkerColor else markersColor

                    val startX =
                        centerX + (externalRadius * (1 - markersLength) * Math.sin(angle)).toFloat()
                    val startY =
                        centerY + (externalRadius * (1 - markersLength) * Math.cos(angle)).toFloat()

                    val endX = centerX + (externalRadius * Math.sin(angle)).toFloat()
                    val endY = centerY + (externalRadius * Math.cos(angle)).toFloat()

                    canvas.drawLine(startX, startY, endX, endY, paint)
                }

                // Label
                if (rotaryStepValues != null) {
                    val topMostX = centerX + (topMostExternalRadius * Math.sin(angle)).toFloat()
                    val topMostY = centerY + (topMostExternalRadius * Math.cos(angle)).toFloat()
                    val text = rotaryStepValues!![i].toString()
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
                if (i == startPoint || i == endPoint) {
                    paint.strokeWidth = stateMarkersWidth.toFloat()
                    paint.color = if (selected) selectedStateMarkerColor else markersColor
                    val startX =
                        centerX + (externalRadius * (1 - markersLength) * Math.sin(angle)).toFloat()
                    val startY =
                        centerY + (externalRadius * (1 - markersLength) * Math.cos(angle)).toFloat()
                    val endX = centerX + (externalRadius * Math.sin(angle)).toFloat()
                    val endY = centerY + (externalRadius * Math.cos(angle)).toFloat()
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
                // Label
                if (i == startPoint || i == centerPoint || i == endPoint) {
                    val topMostX = centerX + (topMostExternalRadius * Math.sin(angle)).toFloat()
                    val topMostY = centerY + (topMostExternalRadius * Math.cos(angle)).toFloat()
                    val text = rotaryContinuousStopPointUpdated!![i].toString()
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
            centerX + (rotaryRadius * (1 - indicatorRelativeLength) * Math.sin(currentAngle)).toFloat()
        val startY =
            centerY + (rotaryRadius * (1 - indicatorRelativeLength) * Math.cos(currentAngle)).toFloat()
        val endX = centerX + (rotaryRadius * Math.sin(currentAngle)).toFloat()
        val endY = centerY + (rotaryRadius * Math.cos(currentAngle)).toFloat()
        canvas.drawLine(startX, startY, endX, endY, paint)
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

//        showPopupWindow(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var wms:Int = widthMeasureSpec
        var hms:Int = heightMeasureSpec

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val r = Resources.getSystem()

        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            widthSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, r.displayMetrics).toInt()
            wms = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
        }

        if (heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST) {
            heightSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, r.displayMetrics).toInt()
            hms = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        }

        super.onMeasure(wms, hms)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = width
        val height = height
        tooltipRadius = Math.max(width, height) * 0.5f
        topMostExternalRadius = tooltipRadius * 0.5f
        externalRadius = topMostExternalRadius * 0.8f
        rotaryRadius = externalRadius * 0.8f
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
    }

}