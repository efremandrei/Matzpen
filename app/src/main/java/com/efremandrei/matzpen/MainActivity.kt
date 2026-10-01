package com.efremandrei.matzpen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.BidiFormatter
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("local_preferences", MODE_PRIVATE) }
    private val store by lazy { ContentStore(this) }
    private lateinit var data: ElectionData
    private val answers = mutableMapOf<String, VoterAnswer>()
    private var lang = "he"
    private var theme = ThemeMode.DARK
    private var screen = "home"
    private var depth = QuestionDepth.BALANCED
    private var questionIndex = 0
    private var selectedList: String? = null
    private var detailReturn = "results"
    private var detailFilter = "all"
    private var showAllRanked = false
    private var showUnranked = false
    private var listFilter = "all"
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView

    private val navy get() = when (theme) { ThemeMode.DARK -> Color.rgb(11, 20, 36); ThemeMode.LIGHT -> Color.rgb(245, 247, 248); ThemeMode.ISRAELI -> Color.rgb(237, 244, 255) }
    private val surface get() = if (theme == ThemeMode.DARK) Color.rgb(23, 38, 58) else Color.WHITE
    private val foreground get() = when (theme) { ThemeMode.DARK -> Color.rgb(243, 247, 250); ThemeMode.LIGHT -> Color.rgb(23, 36, 53); ThemeMode.ISRAELI -> Color.rgb(18, 58, 120) }
    private val muted get() = when (theme) { ThemeMode.DARK -> Color.rgb(185, 201, 214); ThemeMode.LIGHT -> Color.rgb(82, 98, 115); ThemeMode.ISRAELI -> Color.rgb(70, 97, 132) }
    private val accent get() = when (theme) { ThemeMode.DARK -> Color.rgb(82, 219, 198); ThemeMode.LIGHT -> Color.rgb(8, 124, 114); ThemeMode.ISRAELI -> Color.rgb(9, 81, 184) }
    private val border get() = when (theme) { ThemeMode.DARK -> Color.rgb(43, 64, 83); ThemeMode.LIGHT -> Color.rgb(226, 232, 236); ThemeMode.ISRAELI -> Color.rgb(198, 217, 245) }
    private val amber get() = if (theme == ThemeMode.DARK) Color.rgb(247, 186, 106) else Color.rgb(134, 84, 0)
    private val primaryFill get() = if (theme == ThemeMode.ISRAELI) Color.rgb(9, 81, 184) else Color.rgb(45, 211, 186)
    private val primaryText get() = if (theme == ThemeMode.ISRAELI) Color.WHITE else Color.rgb(11, 20, 36)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lang = prefs.getString("language", "he") ?: "he"
        theme = ThemeMode.restore(prefs.getString("theme_mode", null), prefs.getBoolean("dark", true), prefs.getBoolean("israeli_palette", false))
        prefs.edit().putString("theme_mode", theme.id).remove("dark").remove("israeli_palette").apply()
        questionIndex = prefs.getInt("question_index", 0)
        val saved = JSONObject(prefs.getString("answers", "{}") ?: "{}")
        saved.keys().forEach { id ->
            val answer = saved.optJSONObject(id) ?: return@forEach
            val value = answer.optInt("value", 99)
            if (value in -2..2) answers[id] = VoterAnswer(value, answer.optBoolean("priority"))
        }
        data = store.load()
        val storedDepth = prefs.getString("question_depth", null)
        depth = if (!QuestionPlan.supports(data)) QuestionDepth.FULL else
            QuestionDepth.fromId(storedDepth) ?: if (answers.isNotEmpty()) QuestionDepth.FULL else QuestionDepth.BALANCED
        val plan = planQuestions()
        questionIndex = if (storedDepth == null && answers.isNotEmpty()) {
            val legacyId = data.questions.getOrNull(questionIndex)?.id
            plan.indexOfFirst { it.id == legacyId }.coerceAtLeast(0)
        } else questionIndex.coerceIn(0, plan.lastIndex)
        prefs.edit().putString("question_depth", depth.id).putInt("question_index", questionIndex).apply()
        show("home")
        Thread {
            try {
                val updated = store.refresh(data.revision)
                if (updated != null) runOnUiThread {
                    data = updated
                    if (!QuestionPlan.supports(data)) depth = QuestionDepth.FULL
                    questionIndex = questionIndex.coerceIn(0, planQuestions().lastIndex)
                    show(screen, true)
                }
            } catch (_: Exception) { /* Valid bundled or cached data stays available offline. */ }
        }.start()
    }

    private fun tr(en: String, he: String, ar: String) = when (lang) { "he" -> he; "ar" -> ar; else -> en }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun planQuestions() = QuestionPlan.questions(data, depth)
    private fun planAnsweredCount() = planQuestions().count { answers.containsKey(it.id) }
    private fun firstUnansweredIndex(): Int = planQuestions().indexOfFirst { !answers.containsKey(it.id) }.coerceAtLeast(0)
    private fun answeredCount() = data.questions.count { answers.containsKey(it.id) }
    private fun priorityCount() = data.questions.count { answers[it.id]?.priority == true }
    private fun percent(value: Double) = wrapped("${(value * 100).roundToInt()}%")
    private fun wrapped(value: String) = BidiFormatter.getInstance(lang != "en").unicodeWrap(value)
    private fun goToQuestion(index: Int) {
        questionIndex = index.coerceIn(0, planQuestions().lastIndex)
        prefs.edit().putInt("question_index", questionIndex).apply()
        show("question")
    }
    private fun setDepth(next: QuestionDepth, openQuestion: Boolean = false) {
        if (!QuestionPlan.supports(data)) return
        depth = next
        questionIndex = firstUnansweredIndex()
        prefs.edit().putString("question_depth", depth.id).putInt("question_index", questionIndex).apply()
        if (openQuestion) goToQuestion(questionIndex) else show("home", true)
    }
    private fun depthName(value: QuestionDepth) = when (value) {
        QuestionDepth.QUICK -> tr("Quick", "מהיר", "سريع")
        QuestionDepth.BALANCED -> tr("Balanced", "מאוזן", "متوازن")
        QuestionDepth.FULL -> tr("Full", "מלא", "كامل")
    }
    private fun depthDescription() = when (depth) {
        QuestionDepth.QUICK -> tr("Broad overview · ~3 min", "מבט רחב · כ־3 דקות", "نظرة عامة · نحو 3 دقائق")
        QuestionDepth.BALANCED -> tr("More detail · ~4 min", "יותר פירוט · כ־4 דקות", "تفاصيل أكثر · نحو 4 دقائق")
        QuestionDepth.FULL -> tr("All issues · ~5 min", "כל הנושאים · כ־5 דקות", "كل المسائل · نحو 5 دقائق")
    }
    private fun saveAnswers() {
        val objectValue = JSONObject()
        answers.forEach { (id, answer) -> objectValue.put(id, JSONObject().put("value", answer.value).put("priority", answer.priority)) }
        prefs.edit().putString("answers", objectValue.toString()).apply()
    }

    private fun box(color: Int, radius: Int = 18): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), border)
    }

    private fun touchBackground(color: Int, radius: Int = 14): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(Color.argb(48, Color.red(foreground), Color.green(foreground), Color.blue(foreground))), box(color, radius), null)

    private fun text(value: String, size: Float = 16f, color: Int = foreground, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.START
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun addText(parent: LinearLayout, value: String, size: Float = 16f, color: Int = foreground, bold: Boolean = false, top: Int = 0): TextView {
        val view = text(value, size, color, bold)
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) })
        return view
    }

    private fun button(label: String, primary: Boolean = false, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setTextColor(if (primary) primaryText else this@MainActivity.foreground)
        background = touchBackground(if (primary) primaryFill else surface)
        stateListAnimator = null
        elevation = 0f
        minHeight = dp(52)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { onClick() }
    }

    private fun addButton(parent: LinearLayout, label: String, primary: Boolean = false, top: Int = 10, onClick: () -> Unit) {
        parent.addView(button(label, primary, onClick), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) })
    }

    private fun card(parent: LinearLayout, content: (LinearLayout) -> Unit): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(surface, 22)
            setPadding(dp(17), dp(16), dp(17), dp(16))
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content(view)
        return view
    }

    private fun link(parent: LinearLayout, title: String, url: String) {
        if (!url.startsWith("https://")) return
        val view = addText(parent, title + " ↗", 14f, accent, true, top = 8)
        view.minHeight = dp(48)
        view.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        view.isFocusable = true
        view.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun show(target: String, keepScroll: Boolean = false) {
        val previousScroll = if (keepScroll && ::scroll.isInitialized) scroll.scrollY else 0
        val previousScreen = screen
        screen = target
        window.statusBarColor = navy
        window.navigationBarColor = navy
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(navy) }
        root.layoutDirection = if (lang == "en") View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (theme == ThemeMode.DARK) 0 else mask, mask)
        } else {
            val mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = if (theme == ThemeMode.DARK) 0 else mask
        }
        renderHeader()
        scroll = ScrollView(this).apply { isFillViewport = true }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(30))
        }
        scroll.addView(body)
        when (target) {
            "question" -> renderQuestion()
            "results" -> renderResults()
            "lists" -> renderLists()
            "detail" -> renderDetail()
            "about" -> renderAbout()
            else -> renderHome()
        }
        if (target == "question") renderQuestionFooter()
        if (previousScroll > 0) scroll.post { scroll.scrollTo(0, previousScroll) }
        val animationScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (!keepScroll && (target != previousScreen || target == "question") && animationScale > 0f) {
            body.alpha = 0f
            body.translationY = dp(8).toFloat()
            body.animate().alpha(1f).translationY(0f).setDuration((180f * animationScale).toLong().coerceAtMost(450L)).start()
        }
    }

    private fun renderHeader() {
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(header)
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(9), dp(18), 0)
        }
        header.addView(brand)
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.ic_matzpen_mark); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(32), dp(32)))
        val title = text(tr("Election Compass", "מצפן בחירות", "بوصلة الانتخابات"), 26f, foreground, true)
        brand.addView(title, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(9) })

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(4), dp(15), dp(6))
        }
        header.addView(toolbar)
        listOf(
            Triple(ThemeMode.LIGHT, tr("Light", "בהיר", "فاتح"), R.drawable.ic_theme_sun),
            Triple(ThemeMode.DARK, tr("Dark", "כהה", "داكن"), R.drawable.ic_theme_moon),
            Triple(ThemeMode.ISRAELI, tr("Blue and white", "כחול ולבן", "أزرق وأبيض"), R.drawable.ic_star_of_david)
        ).forEach { (mode, label, icon) ->
            toolbar.addView(themeChoice(mode, label, icon), LinearLayout.LayoutParams(dp(44), dp(44)))
        }
        toolbar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val languageButton = button(when (lang) { "he" -> "עברית"; "ar" -> "العربية"; else -> "English" }, false) { chooseLanguage() }
        languageButton.textSize = 14f
        languageButton.minWidth = 0
        languageButton.minHeight = 0
        languageButton.minimumWidth = 0
        languageButton.minimumHeight = 0
        languageButton.setPadding(dp(10), 0, dp(10), 0)
        languageButton.contentDescription = tr("Choose language", "בחירת שפה", "اختيار اللغة")
        toolbar.addView(languageButton, LinearLayout.LayoutParams(-2, dp(40)))
    }

    private fun themeChoice(mode: ThemeMode, label: String, iconId: Int): ImageButton {
        val selected = theme == mode
        val fill = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (selected) Color.argb(48, Color.red(accent), Color.green(accent), Color.blue(accent)) else Color.TRANSPARENT)
        }
        return ImageButton(this).apply {
            setImageResource(iconId)
            imageTintList = ColorStateList.valueOf(if (selected) accent else muted)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = RippleDrawable(ColorStateList.valueOf(accent), fill, null)
            isSelected = selected
            contentDescription = tr(
                "$label theme${if (selected) ", selected" else ""}",
                "ערכת $label${if (selected) ", נבחרה" else ""}",
                "مظهر $label${if (selected) "، محدد" else ""}"
            )
            setOnClickListener {
                if (theme != mode) {
                    theme = mode
                    prefs.edit().putString("theme_mode", mode.id).apply()
                    show(screen, true)
                }
            }
        }
    }

    private fun chooseLanguage() {
        val ids = arrayOf("he", "ar", "en")
        AlertDialog.Builder(this).setTitle(tr("Choose language", "בחירת שפה", "اختيار اللغة"))
            .setSingleChoiceItems(arrayOf("עברית", "العربية", "English"), ids.indexOf(lang)) { dialog, index ->
                lang = ids[index]
                prefs.edit().putString("language", lang).apply()
                dialog.dismiss()
                show(screen, true)
            }.setNegativeButton(tr("Cancel", "ביטול", "إلغاء"), null).show()
    }

    private fun renderHome() {
        addText(body, tr("Unsure who to vote for?", "מתלבטים למי להצביע?", "محتارون لمن تصوّتون؟"), 28f, foreground, true, 18)
        addText(body, tr("Find your match with a quiz", "בואו לגלות דרך שאלון התאמה", "اكتشفوا توافقكم باستبيان"), 19f, muted, top = 8)
        addText(body, tr("Questionnaire length", "אורך השאלון", "طول الاستبيان"), 17f, foreground, true, 22)
        if (QuestionPlan.supports(data)) {
            val options = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            body.addView(options, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
            QuestionDepth.entries.forEach { option ->
                val selected = depth == option
                val label = "${depthName(option)}\n${option.questionCount}"
                val control = button(label, selected) { setDepth(option) }.apply {
                    textSize = 13f
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    contentDescription = tr(
                        "${depthName(option)}, ${option.questionCount} questions${if (selected) ", selected" else ""}",
                        "${depthName(option)}, ${option.questionCount} שאלות${if (selected) ", נבחר" else ""}",
                        "${depthName(option)}، ${option.questionCount} سؤالًا${if (selected) "، محدد" else ""}"
                    )
                }
                options.addView(control, LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginEnd = dp(5) })
            }
        }
        addText(body, depthDescription(), 14f, accent, true, 10)
        addText(body, tr("More answers broaden the comparison.", "יותר תשובות מרחיבות את ההשוואה.", "إجابات أكثر توسّع المقارنة."), 13f, muted, top = 5)
        addText(body, tr("Source coverage still matters.", "כיסוי המקורות עדיין חשוב.", "تغطية المصادر ما زالت مهمة."), 13f, muted, top = 3)
        val count = answeredCount()
        val planAnswered = planAnsweredCount()
        addButton(body, if (planAnswered == planQuestions().size) tr("Review my answers", "עיון בתשובות שלי", "راجع إجاباتي") else if (count > 0) tr("Continue questionnaire", "המשיכו בשאלון", "تابع الاستبيان") else tr("Start questionnaire", "התחילו בשאלון", "ابدأ الاستبيان"), true, 23) {
            goToQuestion(if (planAnswered == planQuestions().size) 0 else firstUnansweredIndex())
        }
        if (count > 0) {
            addText(body, tr("$planAnswered / ${planQuestions().size} answered in this path", "נענו $planAnswered / ${planQuestions().size} במסלול הזה", "أُجيب عن $planAnswered / ${planQuestions().size} في هذا المسار"), 13f, muted, top = 8)
            addText(body, tr("$count answer${if (count == 1) "" else "s"} saved overall", "${if (count == 1) "תשובה אחת נשמרה" else "$count תשובות נשמרו"} בסך הכול", "حُفظت $count إجابات إجمالًا"), 13f, muted, top = 2)
        } else {
            addText(body, tr("Private on this device.", "פרטי במכשיר הזה.", "خاص على هذا الجهاز."), 13f, muted, top = 8)
            addText(body, tr("Answers save automatically.", "התשובות נשמרות אוטומטית.", "تُحفظ الإجابات تلقائيًا."), 13f, muted, top = 2)
        }
        if (count >= 8) addButton(body, tr("View my matches", "הצגת ההתאמות שלי", "عرض التوافقات")) { show("results") }
        addText(body, tr("Explore", "לגלות", "استكشف"), 19f, foreground, true, 30)
        addButton(body, tr("Browse election lists", "עיון ברשימות", "تصفّح القوائم")) { show("lists") }
        addButton(body, tr("About and sources", "על האפליקציה ומקורות", "حول التطبيق والمصادر")) { show("about") }
        card(body) { c ->
            addText(c, tr("Election data", "מידע על הבחירות", "بيانات الانتخابات"), 17f, foreground, true)
            addText(c, tr("26th Knesset · 27 October 2026", "הכנסת ה־26 · 27 באוקטובר 2026", "الكنيست السادس والعشرون · 27 أكتوبر 2026"), 14f, muted, top = 7)
            addText(c, tr("${data.lists.size} submitted lists", "${data.lists.size} רשימות שהוגשו", "${data.lists.size} قائمة مقدّمة"), 14f, muted, top = 5)
            addText(c, tr("Updated ${data.updatedAt}", "עודכן ${data.updatedAt}", "حُدّثت ${data.updatedAt}"), 14f, muted, top = 3)
            addText(c, tr("Some lists are under court review.", "חלק מהרשימות בבדיקה משפטית.", "بعض القوائم قيد المراجعة القضائية."), 13f, amber, top = 10)
            addText(c, tr("Check official information before voting.", "בדקו מידע רשמי לפני ההצבעה.", "تحقّق من المعلومات الرسمية قبل التصويت."), 13f, amber, top = 3)
        }
        addText(body, tr("Alignment is not a voting recommendation.", "התאמה אינה המלצת הצבעה.", "التوافق ليس توصية بالتصويت."), 13f, muted, top = 18)
        addText(body, tr("Your answers stay on this device.", "התשובות נשמרות במכשיר הזה.", "تبقى إجاباتك على هذا الجهاز."), 13f, muted, top = 3)
    }

    private fun renderQuestion() {
        val plan = planQuestions()
        val q = plan[questionIndex]
        addText(body, tr("${depthName(depth)} · Question ${questionIndex + 1} / ${plan.size}", "${depthName(depth)} · שאלה ${questionIndex + 1} / ${plan.size}", "${depthName(depth)} · السؤال ${questionIndex + 1} / ${plan.size}"), 14f, accent, true, 8)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = plan.size
            this.progress = questionIndex + 1
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(border)
            contentDescription = tr("Question ${questionIndex + 1} of ${plan.size}", "שאלה ${questionIndex + 1} מתוך ${plan.size}", "السؤال ${questionIndex + 1} من ${plan.size}")
        }
        body.addView(progress, LinearLayout.LayoutParams(-1, dp(5)).apply { topMargin = dp(8) })
        val jump = addText(body, tr("${planAnsweredCount()} answered here · Jump to question", "נענו כאן ${planAnsweredCount()} · מעבר לשאלה", "أُجيب عن ${planAnsweredCount()} هنا · انتقل إلى سؤال"), 13f, muted, top = 8)
        jump.minHeight = dp(48)
        jump.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        jump.isFocusable = true
        jump.setOnClickListener { showQuestionIndex() }
        card(body) { c ->
            addText(c, q.text(lang), 23f, foreground, true)
            link(c, tr("Original issue and context", "הנושא וההסבר המקוריים", "السؤال الأصلي والسياق"), q.sourceUrl)
        }
        addText(body, tr("Your view", "העמדה שלכם", "رأيك"), 19f, foreground, true, 22)
        addText(body, tr("How much do you agree?", "עד כמה אתם מסכימים?", "إلى أي مدى توافق؟"), 14f, muted, top = 4)
        val labels = arrayOf(
            tr("Strongly disagree", "מתנגדים מאוד", "أعارض بشدة"),
            tr("Disagree", "מתנגדים", "أعارض"),
            tr("Unsure / mixed", "לא בטוחים / מעורב", "غير متأكد / موقف مختلط"),
            tr("Agree", "מסכימים", "أوافق"),
            tr("Strongly agree", "מסכימים מאוד", "أوافق بشدة")
        )
        for (value in -2..2) {
            val chosen = answers[q.id]?.value == value
            addButton(body, (if (chosen) "✓  " else "") + labels[value + 2], chosen, 6) {
                answers[q.id] = VoterAnswer(value, answers[q.id]?.priority ?: false)
                saveAnswers(); show("question", true)
            }
        }
        val priority = CheckBox(this).apply {
            text = tr("Especially important to me · ${priorityCount()} / 3", "חשוב לי במיוחד · ${priorityCount()} / 3", "مهم جدًا بالنسبة لي · ${priorityCount()} / 3")
            setTextColor(this@MainActivity.foreground)
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            minHeight = dp(54)
            isChecked = answers[q.id]?.priority == true
            setOnCheckedChangeListener { _, checked ->
                val answer = answers[q.id]
                if (answer == null) {
                    isChecked = false
                    Toast.makeText(this@MainActivity, tr("Choose an answer first", "בחרו תשובה תחילה", "اختر إجابة أولًا"), Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                if (checked && priorityCount() >= 3) {
                    isChecked = false
                    Toast.makeText(this@MainActivity, tr("You can choose up to 3 priorities", "אפשר לבחור עד 3 נושאים חשובים", "يمكن اختيار حتى 3 أولويات"), Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                answers[q.id] = answer.copy(priority = checked)
                saveAnswers()
                show("question", true)
            }
        }
        body.addView(priority, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) })
        addText(body, tr("Priorities count twice.", "נושאים חשובים נספרים פעמיים.", "تُحتسب الأولويات مرتين."), 13f, muted, top = 3)
    }

    private fun showQuestionIndex() {
        val items = planQuestions().mapIndexed { index, q ->
            "${if (answers.containsKey(q.id)) "✓" else "○"}  ${index + 1}. ${q.text(lang)}"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle(tr("Questions", "השאלות", "الأسئلة"))
            .setItems(items) { dialog, index -> dialog.dismiss(); goToQuestion(index) }
            .setNegativeButton(tr("Close", "סגירה", "إغلاق"), null).show()
    }

    private fun renderQuestionFooter() {
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(10), dp(20), dp(12))
            setBackgroundColor(navy)
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        val previous = button(tr("Previous", "הקודמת", "السابق")) { goToQuestion(questionIndex - 1) }
        previous.isEnabled = questionIndex > 0
        previous.alpha = if (previous.isEnabled) 1f else .45f
        footer.addView(previous, LinearLayout.LayoutParams(0, dp(54), 1f))
        val skip = button(tr("Skip", "דילוג", "تخطَّ")) {
            answers.remove(planQuestions()[questionIndex].id)
            saveAnswers()
            nextQuestion()
        }
        footer.addView(skip, LinearLayout.LayoutParams(0, dp(54), .8f).apply { marginStart = dp(6) })
        val last = questionIndex == planQuestions().lastIndex
        val next = button(if (last) tr("See matches", "הצגת התאמות", "عرض التوافقات") else tr("Next question", "השאלה הבאה", "السؤال التالي"), true) { nextQuestion() }
        next.isEnabled = answers.containsKey(planQuestions()[questionIndex].id)
        next.alpha = if (next.isEnabled) 1f else .45f
        footer.addView(next, LinearLayout.LayoutParams(0, dp(54), 1.6f).apply { marginStart = dp(6) })
    }

    private fun nextQuestion() {
        if (questionIndex < planQuestions().lastIndex) goToQuestion(questionIndex + 1) else show("results")
    }

    private fun renderResults() {
        val count = answeredCount()
        addText(body, tr("Your policy matches", "ההתאמות המדיניות שלכם", "توافقك مع السياسات"), 27f, foreground, true, 12)
        addText(body, tr("$count / ${data.questions.size} answered · ${priorityCount()} priorities", "נענו $count / ${data.questions.size} · ${priorityCount()} נושאים חשובים", "أُجيب عن $count / ${data.questions.size} · ${priorityCount()} أولويات"), 14f, muted, top = 7)
        addText(body, tr("${depthName(depth)} · ${planAnsweredCount()} / ${planQuestions().size}", "${depthName(depth)} · ${planAnsweredCount()} / ${planQuestions().size}", "${depthName(depth)} · ${planAnsweredCount()} / ${planQuestions().size}"), 13f, accent, top = 4)
        if (count < 8) {
            card(body) { c ->
                addText(c, tr("A few more answers will help.", "עוד כמה תשובות יעזרו.", "ستفيد بعض الإجابات الإضافية."), 18f, foreground, true)
                addText(c, tr("Ranked matches start after 8 answers.", "דירוג מופיע אחרי 8 תשובות.", "يبدأ الترتيب بعد 8 إجابات."), 14f, muted, top = 8)
            }
            addButton(body, tr("Answer another question", "ענו על שאלה נוספת", "أجب عن سؤال آخر"), true) { goToQuestion(firstUnansweredIndex()) }
            return
        }
        addText(body, tr("Scores use documented positions.", "הציונים מבוססים על עמדות מתועדות.", "تستند الدرجات إلى مواقف موثّقة."), 14f, muted, top = 12)
        addText(body, tr("Alignment is not a voting recommendation.", "התאמה אינה המלצת הצבעה.", "التوافق ليس توصية بالتصويت."), 14f, muted, top = 3)
        val explainer = addText(body, tr("How matching works", "איך נקבעת ההתאמה", "كيف يُحسب التوافق"), 14f, accent, true, 9)
        explainer.minHeight = dp(48)
        explainer.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        explainer.isFocusable = true
        explainer.setOnClickListener { showMatchingExplanation() }
        val results = ScoreEngine.results(data, answers)
        val ranked = results.filter { it.score != null }
        val unranked = results.filter { it.score == null }
        addText(body, tr("Ranked matches", "התאמות מדורגות", "التوافقات المرتبة"), 19f, foreground, true, 15)
        if (ranked.isEmpty()) card(body) { c -> addText(c, tr("No list has enough documented positions for a rank with these answers.", "אין רשימה עם די עמדות מתועדות לדירוג לפי התשובות האלה.", "لا تملك أي قائمة مواقف موثّقة كافية للترتيب وفق هذه الإجابات."), 15f) }
        var lastScore: Int? = null
        var rank = 0
        ranked.forEachIndexed { index, result ->
            if (result.score != lastScore) { rank = index + 1; lastScore = result.score }
            if (showAllRanked || index < 3) resultCard(result, rank)
        }
        if (ranked.size > 3) addButton(body, if (showAllRanked) tr("Show fewer", "הצגת פחות", "عرض أقل") else tr("Show all ${ranked.size} ranked lists", "הצגת כל ${ranked.size} הרשימות המדורגות", "عرض القوائم المرتبة الـ${ranked.size}"), top = 12) {
            showAllRanked = !showAllRanked; show("results", true)
        }
        addText(body, tr("Not enough evidence", "אין מספיק ראיות", "أدلة غير كافية"), 19f, foreground, true, 28)
        addText(body, tr("You can inspect every list.", "אפשר לעיין בכל רשימה.", "يمكنك فحص كل قائمة."), 13f, muted, top = 5)
        addText(body, tr("Missing positions are not disagreements.", "עמדה חסרה אינה חוסר הסכמה.", "المواقف الناقصة ليست اختلافًا."), 13f, muted, top = 3)
        addButton(body, if (showUnranked) tr("Hide ${unranked.size} lists", "הסתרת ${unranked.size} רשימות", "إخفاء ${unranked.size} قوائم") else tr("View ${unranked.size} lists", "הצגת ${unranked.size} רשימות", "عرض ${unranked.size} قوائم"), top = 12) {
            showUnranked = !showUnranked; show("results", true)
        }
        if (showUnranked) unranked.forEach { resultCard(it, null) }
        val nextDepth = when (depth) {
            QuestionDepth.QUICK -> QuestionDepth.BALANCED
            QuestionDepth.BALANCED -> QuestionDepth.FULL
            QuestionDepth.FULL -> null
        }
        if (nextDepth != null && QuestionPlan.supports(data)) {
            addText(body, tr("More questions show more of your views.", "עוד שאלות מציגות יותר מהעמדות שלכם.", "أسئلة أكثر تُظهر مزيدًا من آرائك."), 13f, muted, top = 24)
            addText(body, tr("Source coverage still limits each score.", "כיסוי המקורות עדיין מגביל כל ציון.", "تغطية المصادر تحدّ كل درجة."), 13f, muted, top = 3)
            addButton(body, tr("Add ${nextDepth.questionCount - depth.questionCount} questions · ${depthName(nextDepth)}", "עוד ${nextDepth.questionCount - depth.questionCount} שאלות · ${depthName(nextDepth)}", "أضف ${nextDepth.questionCount - depth.questionCount} أسئلة · ${depthName(nextDepth)}"), true, 10) { setDepth(nextDepth, true) }
        }
        addButton(body, tr("Edit answers", "עריכת תשובות", "تعديل الإجابات"), top = 20) { goToQuestion(questionIndex) }
        addText(body, tr("Data updated ${data.updatedAt}", "המידע עודכן ${data.updatedAt}", "حُدّثت البيانات ${data.updatedAt}"), 13f, muted, top = 18)
        addText(body, tr("Check official information before voting.", "בדקו מידע רשמי לפני ההצבעה.", "تحقّق من المعلومات الرسمية قبل التصويت."), 13f, muted, top = 3)
    }

    private fun resultCard(result: MatchResult, rank: Int?) {
        val scoreText = result.score?.let { wrapped("$it%") }
        val tile = card(body) { c ->
            addText(c, (if (rank == null) "" else "$rank. ") + wrapped(result.list.name(lang)) + "  ·  " + wrapped(result.list.ballot), 18f, foreground, true)
            addText(c, if (scoreText == null) tr("Unranked", "ללא דירוג", "غير مرتبة") else tr("$scoreText alignment", "$scoreText התאמה", "توافق $scoreText"), 17f, if (result.score == null) muted else accent, true, 6)
            addText(c, tr("Evidence coverage ${percent(result.coverage)}", "כיסוי ראיות ${percent(result.coverage)}", "تغطية الأدلة ${percent(result.coverage)}"), 13f, muted, top = 4)
            if (result.list.status == "court_review") addText(c, tr("Eligibility under court review", "כשירות בבדיקה משפטית", "الأهلية قيد المراجعة القضائية"), 13f, amber, top = 5)
            addText(c, tr("Compare positions  ›", "השוואת עמדות  ‹", "قارن المواقف  ‹"), 13f, accent, true, 8)
        }
        tile.isClickable = true
        tile.isFocusable = true
        tile.background = touchBackground(surface, 22)
        val spokenScore = scoreText ?: tr("unranked", "ללא דירוג", "غير مرتبة")
        tile.contentDescription = tr(
            "${result.list.name(lang)}, $spokenScore alignment, ${percent(result.coverage)} evidence coverage. Compare positions.",
            "${result.list.name(lang)}, התאמה $spokenScore, כיסוי ראיות ${percent(result.coverage)}. השוואת עמדות.",
            "${result.list.name(lang)}، توافق $spokenScore، تغطية الأدلة ${percent(result.coverage)}. قارن المواقف."
        )
        tile.setOnClickListener { openDetail(result.list.id, "results") }
    }

    private fun showMatchingExplanation() {
        AlertDialog.Builder(this).setTitle(tr("How matching works", "איך נקבעת ההתאמה", "كيف يُحسب التوافق"))
            .setMessage(tr(
                "After 8 answers, each documented list position is compared with your answer. A priority counts twice. The percentage averages agreement only where a qualifying source exists. Lists need at least 70% weighted evidence coverage to be ranked; ties share a rank. Unknown positions are not disagreements. This is policy alignment, not a voting recommendation.",
                "לאחר 8 תשובות, כל עמדה מתועדת של רשימה מושווית לתשובתכם. נושא חשוב מקבל משקל כפול. האחוז הוא ממוצע ההסכמה רק כאשר קיים מקור מתאים. לדירוג נדרש כיסוי ראיות משוקלל של 70% לפחות; ציונים זהים חולקים דירוג. עמדה לא ידועה אינה חוסר הסכמה. זו התאמה מדינית, לא המלצת הצבעה.",
                "بعد 8 إجابات، يُقارَن كل موقف موثّق للقائمة بإجابتك. تُحتسب الأولوية بوزن مضاعف. النسبة هي متوسط التوافق فقط عند وجود مصدر مؤهل. يلزم توفر أدلة مرجّحة بنسبة 70٪ على الأقل للترتيب؛ والنتائج المتساوية تشترك في المرتبة. الموقف المجهول ليس اختلافًا. هذا توافق سياسي وليس توصية بالتصويت."
            )).setPositiveButton(tr("Done", "סיום", "تم"), null).show()
    }

    private fun renderLists() {
        addText(body, tr("Election lists", "רשימות הבחירות", "القوائم الانتخابية"), 27f, foreground, true, 12)
        addText(body, tr("${data.lists.size} submitted lists", "${data.lists.size} רשימות שהוגשו", "${data.lists.size} قائمة مقدّمة"), 14f, muted, top = 7)
        addText(body, tr("Snapshot ${data.updatedAt}", "תמונת מצב ${data.updatedAt}", "بيانات ${data.updatedAt}"), 13f, muted, top = 3)
        val search = EditText(this).apply {
            hint = tr("Search name or ballot letters", "חיפוש שם או אותיות פתק", "ابحث بالاسم أو حروف الاقتراع")
            textSize = 16f
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(muted)
            minHeight = dp(56)
            isSingleLine = true
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = box(surface, 14)
        }
        body.addView(search, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(filters, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val items = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(items, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        fun refresh() {
            filters.removeAllViews()
            items.removeAllViews()
            listOf(
                "all" to tr("All", "הכול", "الكل"),
                "approved" to tr("Approved", "אושרו", "مُقرة"),
                "review" to tr("Under review", "בבדיקה", "قيد المراجعة")
            ).forEach { (id, label) ->
                val chip = button(label, listFilter == id) { listFilter = id; refresh() }.apply { textSize = 12f }
                filters.addView(chip, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
            }
            val query = search.text.toString().trim().lowercase()
            val visible = data.lists.filter { list ->
                (listFilter == "all" || (listFilter == "review") == (list.status == "court_review")) &&
                    (query.isEmpty() || list.he.lowercase().contains(query) || list.en.lowercase().contains(query) || list.ballot.lowercase().contains(query))
            }.sortedBy { it.he }
            if (visible.isEmpty()) card(items) { c -> addText(c, tr("No lists match this search.", "לא נמצאו רשימות מתאימות.", "لا توجد قوائم مطابقة."), 15f) }
            visible.forEach { list ->
                val tile = card(items) { c ->
                    addText(c, wrapped(list.name(lang)) + "  ·  " + wrapped(list.ballot), 17f, foreground, true)
                    addText(c, if (list.status == "court_review") tr("Eligibility under court review", "כשירות בבדיקה משפטית", "الأهلية قيد المراجعة القضائية") else tr("Committee approved", "אושרה בוועדה", "أقرّتها اللجنة"), 13f, if (list.status == "court_review") amber else muted, top = 5)
                    addText(c, tr("View positions and sources  ›", "הצגת עמדות ומקורות  ‹", "عرض المواقف والمصادر  ‹"), 13f, accent, true, 8)
                }
                tile.isClickable = true
                tile.isFocusable = true
                tile.contentDescription = tr("View positions and sources for ${list.name(lang)}", "הצגת עמדות ומקורות של ${list.name(lang)}", "عرض مواقف ومصادر ${list.name(lang)}")
                tile.setOnClickListener { openDetail(list.id, "lists") }
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
        refresh()
    }

    private fun openDetail(id: String, from: String) {
        selectedList = id
        detailReturn = from
        detailFilter = "all"
        show("detail")
    }

    private fun renderDetail() {
        val list = data.lists.firstOrNull { it.id == selectedList } ?: return show("lists")
        addText(body, wrapped(list.name(lang)) + "  ·  " + wrapped(list.ballot), 27f, foreground, true, 12)
        addText(body, if (list.status == "court_review") tr("Eligibility under court review", "כשירות בבדיקה משפטית", "الأهلية قيد المراجعة القضائية") else tr("Committee approved list", "רשימה שאושרה בוועדה", "قائمة أقرّتها اللجنة"), 13f, if (list.status == "court_review") amber else muted, top = 7)
        link(body, tr("List profile and status", "פרופיל הרשימה ומעמדה", "ملف القائمة وحالتها"), list.sourceUrl)
        val result = ScoreEngine.results(data, answers).firstOrNull { it.list.id == list.id }
        if (result != null && answeredCount() >= 8) card(body) { c ->
            val scoreText = result.score?.let { wrapped("$it%") }
            addText(c, if (scoreText == null) tr("Unranked · not enough evidence", "ללא דירוג · אין מספיק ראיות", "غير مرتبة · أدلة غير كافية") else tr("$scoreText policy alignment", "$scoreText התאמה מדינית", "توافق سياسي $scoreText"), 19f, if (result.score == null) muted else accent, true)
            addText(c, tr("Evidence coverage ${percent(result.coverage)}", "כיסוי ראיות ${percent(result.coverage)}", "تغطية الأدلة ${percent(result.coverage)}"), 13f, muted, top = 5)
        }
        val answered = data.questions.filter { answers.containsKey(it.id) }
        if (answered.isNotEmpty()) {
            val aligned = answered.count { issueCategory(answers[it.id], data.positions[list.id to it.id]) == "aligned" }
            val different = answered.count { issueCategory(answers[it.id], data.positions[list.id to it.id]) == "different" }
            val unknown = answered.count { issueCategory(answers[it.id], data.positions[list.id to it.id]) == "unknown" }
            addText(body, tr("Where views meet or differ", "איפה העמדות דומות או שונות", "أين تتوافق الآراء أو تختلف"), 19f, foreground, true, 22)
            addText(body, tr("$aligned broadly aligned · $different different · $unknown without a documented position", "$aligned דומות בקירוב · $different שונות · $unknown ללא עמדה מתועדת", "$aligned متوافقة عمومًا · $different مختلفة · $unknown دون موقف موثّق"), 14f, muted, top = 7)
            addText(body, tr("Broad alignment means the positions are at most one step apart on the answer scale.", "דמיון בקירוב פירושו פער של שלב אחד לכל היותר בסולם התשובות.", "التوافق العام يعني أن الفارق لا يزيد على درجة واحدة في سلّم الإجابات."), 12f, muted, top = 5)
        } else addText(body, tr("Answer questions to compare your views with this list.", "ענו על שאלות כדי להשוות את עמדותיכם לרשימה.", "أجب عن الأسئلة لمقارنة آرائك بهذه القائمة."), 14f, muted, top = 18)
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(filters, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        val items = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(items)
        fun refresh() {
            filters.removeAllViews()
            items.removeAllViews()
            val choices = if (answered.isEmpty()) listOf("all" to tr("All issues", "כל הנושאים", "كل المسائل")) else listOf(
                "all" to tr("All", "הכול", "الكل"),
                "aligned" to tr("Agreement", "דמיון", "توافق"),
                "different" to tr("Differences", "הבדלים", "اختلافات"),
                "unknown" to tr("Unknown", "לא ידוע", "مجهول")
            )
            choices.forEach { (id, label) ->
                val chip = button(label, detailFilter == id) { detailFilter = id; refresh() }.apply { textSize = 11f }
                filters.addView(chip, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) })
            }
            val candidates = if (answered.isEmpty()) data.questions else answered
            val visible = candidates.filter { detailFilter == "all" || issueCategory(answers[it.id], data.positions[list.id to it.id]) == detailFilter }
            if (visible.isEmpty()) card(items) { c -> addText(c, tr("No issues in this section.", "אין נושאים בחלק הזה.", "لا توجد مسائل في هذا القسم."), 15f) }
            visible.forEach { q ->
                val answer = answers[q.id]
                val position = data.positions[list.id to q.id]
                card(items) { c ->
                    addText(c, q.text(lang), 17f, foreground, true)
                    addText(c, tr("Your view", "העמדה שלכם", "رأيك"), 12f, muted, true, 13)
                    addText(c, answer?.let { answerLabel(it.value) } ?: tr("Not answered", "לא נענה", "لم تُجب"), 15f, foreground, top = 3)
                    addText(c, tr("Documented list position", "העמדה המתועדת של הרשימה", "موقف القائمة الموثّق"), 12f, muted, true, 12)
                    addText(c, positionLabel(position?.value), 15f, if (position == null) muted else foreground, top = 3)
                    if (position != null) {
                        addText(c, tr("Source", "מקור", "المصدر"), 12f, muted, true, 12)
                        addText(c, "${sourceType(position.sourceType)} · ${position.sourceDate}", 13f, muted, top = 3)
                        val note = if (lang != "he" && position.sourceTitle.any { it in '\u0590'..'\u05FF' }) tr("Open original Hebrew source", "פתיחת המקור בעברית", "افتح المصدر العبري الأصلي") else tr("Open original source", "פתיחת המקור", "افتح المصدر الأصلي")
                        link(c, note, position.sourceUrl)
                        addText(c, position.sourceTitle, 12f, muted)
                    }
                }
            }
        }
        refresh()
        addButton(body, tr("Back", "חזרה", "رجوع"), top = 18) { show(detailReturn) }
    }

    private fun positionLabel(value: Int?) = when (value) {
        -2 -> tr("Against", "נגד", "ضد")
        0 -> tr("Conditional or mixed", "חלקית או מותנית", "مشروط أو مختلط")
        2 -> tr("For", "בעד", "مع")
        else -> tr("No qualifying source", "אין מקור מתאים", "لا يوجد مصدر مؤهل")
    }
    private fun sourceType(value: String) = when (value) {
        "platform" -> tr("Platform", "מצע", "برنامج")
        "vote" -> tr("Vote", "הצבעה", "تصويت")
        "statement" -> tr("Statement", "הצהרה", "تصريح")
        else -> value
    }
    private fun issueCategory(answer: VoterAnswer?, position: Position?): String = when {
        answer == null -> "unanswered"
        position == null -> "unknown"
        abs(answer.value - position.value) <= 1 -> "aligned"
        else -> "different"
    }
    private fun answerLabel(value: Int) = when (value) {
        -2 -> tr("Strongly disagree", "מתנגדים מאוד", "أعارض بشدة")
        -1 -> tr("Disagree", "מתנגדים", "أعارض")
        0 -> tr("Unsure / mixed", "לא בטוחים / מעורב", "غير متأكد / موقف مختلط")
        1 -> tr("Agree", "מסכימים", "أوافق")
        else -> tr("Strongly agree", "מסכימים מאוד", "أوافق بشدة")
    }

    private fun renderAbout() {
        addText(body, tr("About Election Compass", "על מצפן בחירות", "حول بوصلة الانتخابات"), 27f, foreground, true, 12)
        card(body) { c ->
            addText(c, tr("An independent policy-alignment guide. It does not endorse a party or predict election outcomes.", "כלי עצמאי להשוואת עמדות מדיניות. אינו תומך ברשימה ואינו חוזה תוצאות בחירות.", "دليل مستقل لمقارنة المواقف السياسية. لا يؤيد أي قائمة ولا يتنبأ بنتائج الانتخابات."), 16f)
            addText(c, tr("Your answers are stored only on this device. Public election content is downloaded by HTTPS; no answers or analytics are uploaded.", "התשובות נשמרות במכשיר בלבד. מידע בחירות ציבורי מתקבל ב־HTTPS; אין העלאת תשובות או נתוני שימוש.", "تُحفظ إجاباتك على هذا الجهاز فقط. تُحمّل بيانات الانتخابات العامة عبر HTTPS؛ لا تُرسل الإجابات أو التحليلات."), 14f, muted, top = 13)
        }
        card(body) { c ->
            addText(c, tr("Data and attribution", "מידע וקרדיט", "البيانات والنَسب"), 18f, foreground, true)
            addText(c, tr("Adapted from מצפן הבחירה 2026, dataset ${data.sourceVersion}, CC BY 4.0. Its first 18 policy questions are used; two coalition-strategy questions and positions supported only by third-party reporting are excluded.", "עיבוד של נתוני מצפן הבחירה 2026, גרסה ${data.sourceVersion}, ברישיון CC BY 4.0. נכללו 18 שאלות המדיניות הראשונות; שתי שאלות על שותפות קואליציונית ועמדות המבוססות רק על דיווח צד שלישי הוחרגו.", "مقتبس من بيانات מצפן הבחירה 2026، إصدار ${data.sourceVersion}، بترخيص CC BY 4.0. استُخدمت أول 18 مسألة سياسة؛ واستُبعد سؤالان عن الائتلاف والمواقف المستندة فقط إلى تقارير طرف ثالث."), 13f, muted, top = 8)
            link(c, "bhirot26.online", "https://bhirot26.online")
            link(c, tr("Open dataset", "מאגר הנתונים הפתוח", "البيانات المفتوحة"), "https://github.com/dangelm/bhirot26-election-data")
            link(c, "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/")
        }
        card(body) { c ->
            addText(c, "Andrei Efremuahkin", 17f, foreground, true)
            val email = addText(c, "andrei.efr@gmail.com ↗", 14f, accent, true, 8)
            email.minHeight = dp(48)
            email.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            email.isFocusable = true
            email.setOnClickListener { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:andrei.efr@gmail.com"))) }
            link(c, "github.com/efremandrei/Matzpen", "https://github.com/efremandrei/Matzpen")
            val info = packageManager.getPackageInfo(packageName, 0)
            val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            addText(c, "Version ${info.versionName} · build $build", 13f, muted, top = 8)
        }
        addButton(body, tr("Delete my answers", "מחיקת התשובות שלי", "حذف إجاباتي"), top = 20) {
            AlertDialog.Builder(this).setTitle(tr("Delete answers?", "למחוק את התשובות?", "حذف الإجابات؟"))
                .setMessage(tr("This deletes answers saved on this device. It cannot be undone.", "התשובות השמורות במכשיר הזה יימחקו. לא ניתן לבטל זאת.", "سيؤدي هذا إلى حذف الإجابات المحفوظة على هذا الجهاز، ولا يمكن التراجع."))
                .setNegativeButton(tr("Cancel", "ביטול", "إلغاء"), null)
                .setPositiveButton(tr("Delete", "מחיקה", "حذف")) { _, _ ->
                    answers.clear(); saveAnswers(); questionIndex = 0
                    prefs.edit().putInt("question_index", 0).apply()
                    show("home")
                }.show()
        }
        addButton(body, tr("Home", "בית", "الرئيسية")) { show("home") }
    }

    override fun onBackPressed() {
        when (screen) { "detail" -> show(detailReturn); "home" -> super.onBackPressed(); else -> show("home") }
    }
}
