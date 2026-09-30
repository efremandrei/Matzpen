package com.efremandrei.matzpen

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("local_preferences", MODE_PRIVATE) }
    private val store by lazy { ContentStore(this) }
    private lateinit var data: ElectionData
    private val answers = mutableMapOf<String, VoterAnswer>()
    private var lang = "he"
    private var dark = true
    private var screen = "home"
    private var questionIndex = 0
    private var selectedList: String? = null
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout

    private val navy get() = if (dark) Color.rgb(12, 20, 35) else Color.rgb(246, 249, 252)
    private val surface get() = if (dark) Color.rgb(25, 37, 55) else Color.WHITE
    private val foreground get() = if (dark) Color.rgb(238, 245, 250) else Color.rgb(25, 37, 55)
    private val muted get() = if (dark) Color.rgb(166, 186, 203) else Color.rgb(83, 104, 121)
    private val accent = Color.rgb(25, 171, 154)
    private val amber = Color.rgb(239, 172, 79)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lang = prefs.getString("language", "he") ?: "he"
        dark = prefs.getBoolean("dark", true)
        val saved = JSONObject(prefs.getString("answers", "{}") ?: "{}")
        saved.keys().forEach { id ->
            val answer = saved.optJSONObject(id) ?: return@forEach
            val value = answer.optInt("value", 99)
            if (value in -2..2) answers[id] = VoterAnswer(value, answer.optBoolean("priority"))
        }
        data = store.load()
        show("home")
        Thread {
            try {
                val updated = store.refresh(data.revision)
                if (updated != null) runOnUiThread { data = updated; show(screen) }
            } catch (_: Exception) { /* Valid bundled or cached data stays available offline. */ }
        }.start()
    }

    private fun tr(en: String, he: String, ar: String) = when (lang) { "he" -> he; "ar" -> ar; else -> en }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun saveAnswers() {
        val objectValue = JSONObject()
        answers.forEach { (id, answer) -> objectValue.put(id, JSONObject().put("value", answer.value).put("priority", answer.priority)) }
        prefs.edit().putString("answers", objectValue.toString()).apply()
    }

    private fun box(color: Int, radius: Int = 18): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun text(value: String, size: Float = 16f, color: Int = foreground, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = if (lang == "en") Gravity.START else Gravity.END
        textDirection = if (lang == "en") View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_RTL
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
        setTextColor(if (primary) Color.rgb(8, 28, 33) else this@MainActivity.foreground)
        background = box(if (primary) Color.rgb(57, 216, 188) else surface, 14)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { onClick() }
    }

    private fun addButton(parent: LinearLayout, label: String, primary: Boolean = false, top: Int = 10, onClick: () -> Unit) {
        parent.addView(button(label, primary, onClick), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) })
    }

    private fun card(parent: LinearLayout, content: (LinearLayout) -> Unit) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(surface)
            setPadding(dp(17), dp(16), dp(17), dp(16))
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content(view)
    }

    private fun link(parent: LinearLayout, title: String, url: String) {
        if (!url.startsWith("https://")) return
        val view = addText(parent, title + " ↗", 13f, Color.rgb(62, 211, 190), top = 8)
        view.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun show(target: String) {
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
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else mask, mask)
        } else {
            val mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = if (dark) 0 else mask
        }
        renderHeader()
        val scroll = ScrollView(this).apply { isFillViewport = true }
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
    }

    private fun renderHeader() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(10))
        }
        root.addView(row)
        val title = text("✦  " + tr("Matzpen", "מצפן", "بوصلة"), 23f, foreground, true)
        row.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val languageButton = button(lang.uppercase(), false) {
            lang = when (lang) { "he" -> "ar"; "ar" -> "en"; else -> "he" }
            prefs.edit().putString("language", lang).apply()
            show(screen)
        }
        row.addView(languageButton, LinearLayout.LayoutParams(dp(65), dp(48)))
        val themeButton = button(if (dark) "☀" else "☾", false) {
            dark = !dark
            prefs.edit().putBoolean("dark", dark).apply()
            show(screen)
        }
        row.addView(themeButton, LinearLayout.LayoutParams(dp(54), dp(48)).apply { marginStart = dp(6) })
    }

    private fun renderHome() {
        addText(body, tr("Find your policy fit", "למצוא את ההתאמה המדינית שלך", "اكتشف توافقك مع السياسات"), 29f, foreground, true, 20)
        addText(body, tr(
            "Answer 18 policy questions. See how your views align with election lists, with evidence for every scored position.",
            "ענו על 18 שאלות מדיניות וראו כיצד העמדות שלכם תואמות לרשימות, עם מקור לכל עמדה מנוקדת.",
            "أجب عن 18 سؤالًا حول السياسات، وشاهد مدى توافق آرائك مع القوائم مع مصدر لكل موقف محسوب."
        ), 16f, muted, top = 10)
        card(body) { c ->
            addText(c, tr("Election preview", "תמונת מצב לבחירות", "نظرة على الانتخابات"), 18f, foreground, true)
            addText(c, tr("26th Knesset · 27 October 2026", "הכנסת ה־26 · 27 באוקטובר 2026", "الكنيست السادس والعشرون · 27 أكتوبر 2026"), 14f, muted, top = 7)
            addText(c, tr("38 submitted lists · content updated ${data.updatedAt}", "38 רשימות שהוגשו · המידע עודכן ${data.updatedAt}", "38 قائمة مقدّمة · تحديث البيانات ${data.updatedAt}"), 14f, muted, top = 5)
            addText(c, tr("Some list eligibility decisions are under court review. Check current official information before voting.", "ההכרעה לגבי כשירותן של חלק מהרשימות עדיין נבחנת בבית המשפט. בדקו מידע רשמי עדכני לפני ההצבעה.", "ما زالت أهلية بعض القوائم قيد المراجعة القضائية. تحقّق من المعلومات الرسمية قبل التصويت."), 13f, amber, top = 10)
        }
        addButton(body, tr("Start questionnaire", "התחילו בשאלון", "ابدأ الاستبيان"), true, 22) { questionIndex = 0; show("question") }
        if (answers.isNotEmpty()) addButton(body, tr("View my matches", "הצגת ההתאמות שלי", "عرض التوافقات")) { show("results") }
        addButton(body, tr("Browse all lists", "כל הרשימות", "تصفح جميع القوائم")) { show("lists") }
        addButton(body, tr("About & data sources", "על האפליקציה ומקורות המידע", "حول التطبيق ومصادر البيانات")) { show("about") }
        addText(body, tr("Your answers stay on this device. Alignment is not a voting recommendation.", "התשובות נשמרות במכשיר בלבד. התאמה אינה המלצת הצבעה.", "تبقى إجاباتك على هذا الجهاز. التوافق ليس توصية بالتصويت."), 13f, muted, top = 18)
    }

    private fun renderQuestion() {
        val q = data.questions[questionIndex]
        addText(body, tr("Question ${questionIndex + 1} of 18", "שאלה ${questionIndex + 1} מתוך 18", "السؤال ${questionIndex + 1} من 18"), 14f, accent, true, 8)
        card(body) { c ->
            addText(c, q.text(lang), 21f, foreground, true)
            link(c, tr("Original issue and context", "הנושא וההסבר המקוריים", "السؤال الأصلي والسياق"), q.sourceUrl)
        }
        addText(body, tr("How much do you agree?", "עד כמה אתם מסכימים?", "إلى أي مدى توافق؟"), 16f, muted, top = 22)
        val labels = arrayOf(
            tr("Strongly disagree", "מתנגדים מאוד", "أعارض بشدة"),
            tr("Disagree", "מתנגדים", "أعارض"),
            tr("Unsure / mixed", "לא בטוחים / מעורב", "غير متأكد / موقف مختلط"),
            tr("Agree", "מסכימים", "أوافق"),
            tr("Strongly agree", "מסכימים מאוד", "أوافق بشدة")
        )
        for (value in -2..2) {
            val chosen = answers[q.id]?.value == value
            addButton(body, (if (chosen) "✓  " else "") + labels[value + 2], chosen, 7) {
                answers[q.id] = VoterAnswer(value, answers[q.id]?.priority ?: false)
                saveAnswers(); show("question")
            }
        }
        val priority = CheckBox(this).apply {
            text = tr("Especially important to me (up to 3)", "חשוב לי במיוחד (עד 3)", "مهم جدًا بالنسبة لي (حتى 3)")
            setTextColor(this@MainActivity.foreground)
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            isChecked = answers[q.id]?.priority == true
            setOnCheckedChangeListener { _, checked ->
                val answer = answers[q.id]
                if (answer == null) { isChecked = false; return@setOnCheckedChangeListener }
                if (checked && answers.values.count { it.priority } >= 3) { isChecked = false; return@setOnCheckedChangeListener }
                answers[q.id] = answer.copy(priority = checked)
                saveAnswers()
            }
        }
        body.addView(priority, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) })
        addButton(body, tr("Skip this question", "דילוג על שאלה זו", "تخطَّ هذا السؤال"), top = 4) {
            answers.remove(q.id); saveAnswers(); nextQuestion()
        }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(nav, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        nav.addView(button(tr("Previous", "הקודמת", "السابق")) { questionIndex = (questionIndex - 1).coerceAtLeast(0); show("question") }, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(button(tr("Next / results", "הבאה / תוצאות", "التالي / النتائج"), true) { nextQuestion() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        addButton(body, tr("Back to home", "חזרה לבית", "العودة للرئيسية"), top = 16) { show("home") }
    }

    private fun nextQuestion() {
        if (questionIndex < data.questions.lastIndex) { questionIndex++; show("question") } else show("results")
    }

    private fun renderResults() {
        val count = data.questions.count { answers.containsKey(it.id) }
        addText(body, tr("Your policy matches", "ההתאמות המדיניות שלכם", "توافقك مع السياسات"), 27f, foreground, true, 12)
        addText(body, tr("$count of 18 answered · ${answers.values.count { it.priority }} priorities", "נענו $count מתוך 18 · ${answers.values.count { it.priority }} נושאים חשובים", "أُجيب عن $count من 18 · ${answers.values.count { it.priority }} أولويات"), 14f, muted, top = 7)
        if (count < 8) {
            card(body) { c -> addText(c, tr("Answer at least 8 questions to see ranked matches.", "ענו על 8 שאלות לפחות כדי לראות דירוג.", "أجب عن 8 أسئلة على الأقل لرؤية الترتيب."), 16f) }
            addButton(body, tr("Continue questionnaire", "המשיכו בשאלון", "تابع الاستبيان"), true) { show("question") }
            return
        }
        addText(body, tr("Scores use policy agreement only. Missing evidence reduces coverage, and lists below 70% coverage are not ranked.", "הציונים מבוססים רק על הסכמה בנושאי מדיניות. חוסר ראיות מפחית את הכיסוי, ורשימות עם פחות מ־70% כיסוי אינן מדורגות.", "تعتمد الدرجات على توافق السياسات فقط. يقلّل غياب الأدلة من التغطية، ولا تُرتّب القوائم التي تقل تغطيتها عن 70٪."), 13f, muted, top = 10)
        var lastScore: Int? = null
        var rank = 0
        ScoreEngine.results(data, answers).forEachIndexed { index, result ->
            if (result.score != null && result.score != lastScore) { rank = index + 1; lastScore = result.score }
            card(body) { c ->
                addText(c, (if (result.score != null) "$rank. " else "") + result.list.name(lang) + "  ·  ${result.list.ballot}", 18f, foreground, true)
                addText(c, if (result.score == null) tr("Insufficient evidence", "אין מספיק ראיות", "أدلة غير كافية") else "${result.score}%", 20f, if (result.score == null) muted else accent, true, 7)
                addText(c, tr("Evidence coverage ${(result.coverage * 100).roundToInt()}%", "כיסוי ראיות ${(result.coverage * 100).roundToInt()}%", "تغطية الأدلة ${(result.coverage * 100).roundToInt()}٪"), 13f, muted, top = 3)
                if (result.list.status == "court_review") addText(c, tr("Eligibility under court review", "כשירות בבדיקה משפטית", "الأهلية قيد المراجعة القضائية"), 12f, amber, top = 5)
                addButton(c, tr("Why this result", "למה התוצאה הזאת", "لماذا هذه النتيجة"), top = 9) { selectedList = result.list.id; show("detail") }
            }
        }
        addButton(body, tr("Edit answers", "עריכת תשובות", "تعديل الإجابات"), top = 20) { show("question") }
        addButton(body, tr("Home", "בית", "الرئيسية")) { show("home") }
    }

    private fun renderLists() {
        addText(body, tr("All 38 submitted lists", "כל 38 הרשימות שהוגשו", "جميع القوائم الـ38 المقدّمة"), 26f, foreground, true, 12)
        addText(body, tr("Ballot letters and status reflect the ${data.updatedAt} snapshot.", "אותיות הפתק והמעמד נכונים לתמונת המצב מ־${data.updatedAt}.", "الحروف والحالة حسب بيانات ${data.updatedAt}."), 13f, muted, top = 8)
        data.lists.sortedBy { it.he }.forEach { list ->
            card(body) { c ->
                addText(c, list.name(lang) + "  ·  ${list.ballot}", 18f, foreground, true)
                addText(c, if (list.status == "court_review") tr("Under court review", "בבדיקה משפטית", "قيد المراجعة القضائية") else tr("Committee approved", "אושרה בוועדה", "أقرّتها اللجنة"), 12f, if (list.status == "court_review") amber else muted, top = 5)
                addButton(c, tr("View evidence", "הצגת ראיות", "عرض الأدلة"), top = 8) { selectedList = list.id; show("detail") }
            }
        }
        addButton(body, tr("Home", "בית", "الرئيسية"), top = 20) { show("home") }
    }

    private fun renderDetail() {
        val list = data.lists.firstOrNull { it.id == selectedList } ?: return show("lists")
        addText(body, list.name(lang) + "  ·  ${list.ballot}", 25f, foreground, true, 12)
        addText(body, if (list.status == "court_review") tr("Eligibility under court review", "כשירות בבדיקה משפטית", "الأهلية قيد المراجعة القضائية") else tr("Committee approved list", "רשימה שאושרה בוועדה", "قائمة أقرّتها اللجنة"), 13f, if (list.status == "court_review") amber else muted, top = 7)
        link(body, tr("List profile and status", "פרופיל הרשימה ומעמדה", "ملف القائمة وحالتها"), list.sourceUrl)
        val result = ScoreEngine.results(data, answers).firstOrNull { it.list.id == list.id }
        if (result != null && answers.size >= 8) addText(body, tr("Match: ${result.score?.let { "$it%" } ?: "unranked"} · coverage ${(result.coverage * 100).roundToInt()}%", "התאמה: ${result.score?.let { "$it%" } ?: "ללא דירוג"} · כיסוי ${(result.coverage * 100).roundToInt()}%", "التوافق: ${result.score?.let { "$it%" } ?: "غير مرتبة"} · التغطية ${(result.coverage * 100).roundToInt()}٪"), 16f, accent, true, 14)
        data.questions.forEach { q ->
            val position = data.positions[list.id to q.id]
            card(body) { c ->
                addText(c, q.text(lang), 15f, foreground, true)
                val stance = when (position?.value) {
                    -2 -> tr("Against", "נגד", "ضد")
                    0 -> tr("Mixed / conditional", "עמדה חלקית / מותנית", "موقف جزئي / مشروط")
                    2 -> tr("For", "בעד", "مع")
                    else -> tr("No qualifying source", "אין מקור מתאים", "لا يوجد مصدر مؤهل")
                }
                addText(c, stance, 14f, if (position == null) muted else accent, true, 8)
                if (position != null) {
                    addText(c, "${position.sourceType} · ${position.sourceDate}", 12f, muted, top = 4)
                    link(c, position.sourceTitle, position.sourceUrl)
                }
                answers[q.id]?.let { answer ->
                    addText(c, tr("Your answer: ${answer.value}", "התשובה שלכם: ${answer.value}", "إجابتك: ${answer.value}"), 12f, muted, top = 6)
                }
            }
        }
        addButton(body, tr("Back to results", "חזרה לתוצאות", "العودة للنتائج"), top = 18) { show(if (answers.size >= 8) "results" else "lists") }
    }

    private fun renderAbout() {
        addText(body, tr("About Matzpen", "על מצפן", "حول بوصلة"), 27f, foreground, true, 12)
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
            val email = addText(c, "andrei.efr@gmail.com ↗", 13f, Color.rgb(62, 211, 190), top = 8)
            email.setOnClickListener { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:andrei.efr@gmail.com"))) }
            link(c, "github.com/efremandrei/matzpen", "https://github.com/efremandrei/matzpen")
            val info = packageManager.getPackageInfo(packageName, 0)
            val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            addText(c, "Version ${info.versionName} · build $build", 13f, muted, top = 8)
        }
        addButton(body, tr("Delete my answers", "מחיקת התשובות שלי", "حذف إجاباتي"), top = 20) {
            answers.clear(); saveAnswers(); show("home")
        }
        addButton(body, tr("Home", "בית", "الرئيسية")) { show("home") }
    }

    override fun onBackPressed() {
        when (screen) { "detail" -> show(if (answers.size >= 8) "results" else "lists"); "home" -> super.onBackPressed(); else -> show("home") }
    }
}
