"""Build revision 2 from the pinned election snapshot and reviewed party sources.

Each added stance below is tied to a specific official party document listed
in party_sources.json. An omitted list/question pair means unknown, not no.
"""
from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/content.json"
SOURCES = ROOT / "scripts/party_sources.json"

# Site order is the Central Elections Committee list order, as preserved in
# the indexed page URLs. Keep it explicit so a renamed party cannot be paired
# with another party's source by an approximate name match.
PARTY_IDS = """beyachad yashar sharshar shutafut pirates amcha haskel ganeden kolhanashim byn beytenu mishpat shema otzma seder miluim democrats raam shas tzibur aniveata britolam hatikun gush batach orot bitachon tzeva likud kachol rz ahi tzomet tkuma joint hakahal utj noam""".split()

# Short, original Hebrew paraphrases of each verifiable platform topic. The
# source URL for every point is stored separately in party_sources.json.
PROFILE_POINTS = {
 "beyachad": {
  "ביטחון": "דורשים לפרק את חמאס מנשקו ולבסס מדיניות הרתעה ויוזמה מול איומים אזוריים.",
  "כלכלה ויוקר המחיה": "מציעים להגביר תחרות בענף המזון, להסיר חסמי יבוא ולהפחית מכסים, לצד תמיכה ישירה בחקלאים.",
  "דיור": "מבקשים להגדיל את היצע הדירות והשכירות ארוכת הטווח ולסייע למשרתי מילואים בדיור.",
  "מערכת המשפט": "מציעים חוקה שראשיתה במגילת העצמאות וחוק יסוד שיגדיר את סמכויות רשויות השלטון.",
  "דת ומדינה": "תומכים בברית זוגיות אזרחית ובהסדרת תחבורה בשבת בהחלטת כל רשות מקומית.",
  "חינוך": "מציעים לימודי ליבה כתנאי לתקצוב, יותר סמכות לבתי הספר ושיפור שכר המורים.",
  "בריאות": "מבקשים להרחיב שירותי בריאות בקהילה ולהשקיע ברפואה מונעת.",
  "מדיניות חוץ": "שואפים להרחיב נורמליזציה אזורית, לרבות עם סעודיה, ולבנות שיתוף פעולה מול איראן.",
 },
 "yashar": {
  "ביטחון": "מציעים שירות ממלכתי שיכלול גם חרדים וערבים, וחיזוק הביטחון האישי והגבולות.",
  "כלכלה ויוקר המחיה": "מבקשים לפרק ריכוזיות ולתת עדיפות בהטבות למי שנושאים בנטל השירות והעבודה.",
  "דיור": "תומכים בתוספת היצע דיור, בהתחדשות עירונית ובמס על קרקע שאינה מקודמת לבנייה.",
  "מערכת המשפט": "מדגישים עצמאות משפטית וזכויות מיעוט, ומציעים חוק יסוד החקיקה והגבלת כהונת ראש ממשלה.",
  "דת ומדינה": "מציעים הסדרים מקומיים שיאפשרו לקהילות שונות לשמור על אורח חייהן.",
  "חינוך": "תומכים בלימודי ליבה, בחינוך ממלכתי ובהעברת סמכויות למורים ולמנהלים.",
  "בריאות": "מציעים תוכנית לאומית לשיקום מטראומה ולחיזוק החוסן בקהילה.",
  "מדיניות חוץ": "מבקשים להרחיב את מעגל השלום ולחזק את הקשר עם יהודי התפוצות.",
 },
 "pirates": {
  "חינוך": "מציבים שירותי חינוך לאזרח ולקהילה כחלק משירותים לכל שלבי החיים.",
  "בריאות": "כוללים שירותי בריאות ורווחה במטרות השירות הציבורי לקהילה.",
 "מדיניות חוץ": "מעמידים שלום בין תושבי הארץ כאחד מיעדי המצע.",
  "שיטת ממשל": "מציעים דמוקרטיה נזילה שבה חברי המפלגה מצביעים ישירות או מאצילים את קולם.",
 },
 "haskel": {
  "ביטחון": "מציעים שירות אזרחי למי שאינו משרת בצבא, תגמול למשרתים ופעולה נגד חמאס וחיזבאללה.",
  "כלכלה ויוקר המחיה": "מבקשים לצמצם משרדי ממשלה ולהפחית מס חברות.",
  "חינוך": "תומכים בשוברי חינוך להורים וביותר עצמאות למנהלי בתי הספר.",
  "מדיניות חוץ": "מציעים לבטל רשמית את הסכמי אוסלו.",
  "עמדה קואליציונית": "מעדיפים ממשלת אחדות ציונית עם מפלגות שהרשימה מגדירה ציוניות.",
 },
 "ganeden": {
  "ביטחון": "מציעים מאבק בטרור ובפשיעה לצד הקמת משרד לשלום ולפיוס.",
  "כלכלה ויוקר המחיה": "מציעים שמיטת חובות, הכנסה בסיסית, שינויי מס ותמיכה בעסקים קטנים.",
  "דיור": "מבקשים להרחיב דיור בר השגה ולהקל על תכנון והתחדשות עירונית.",
  "מערכת המשפט": "מציעים להנגיש הליכים משפטיים ולהגביר אחריות אישית של בעלי תפקיד ציבורי.",
  "חינוך": "תומכים בלימודי כלכלה ומשפט בסיסיים ובהרחבת לימודי מלאכה ואמנות.",
  "בריאות": "מציעים תוכנית לבריאות הנפש, טיפול בטראומה וחיזוק הרפואה המונעת.",
  "מדיניות חוץ": "מציעים דיאלוג בין דתי ובריתות אזוריות במסגרת חזון לשלום.",
 },
 "byn": {
  "ביטחון": "מבקשים להיאבק בפשיעה ביישובים ערביים ולעבור מצבא חובה לצבא מקצועי.",
  "כלכלה ויוקר המחיה": "קוראים להפחתת יוקר המחיה ולצמצום פערים כלכליים.",
  "חינוך": "מציבים צמצום פערים וחיזוק החינוך והשירותים הציבוריים כיעדים.",
  "מדיניות חוץ": "תומכים בקידום שלום עם הפלסטינים ועם מדינות ערב על בסיס ביטחון ושוויון.",
 },
 "beytenu": {
  "ביטחון": "מציעים חובת שירות צבאי או אזרחי לבני 18 והגדלת יכולת ההכרעה של צה״ל.",
  "כלכלה ויוקר המחיה": "תומכים בצמצום רגולציה ומשרדי ממשלה, בהגברת תחרות ובפתיחת יבוא.",
  "דיור": "מציעים להגדיל היצע דירות ולזרז תכנון ובנייה ושכירות ארוכת טווח.",
  "מערכת המשפט": "תומכים בחוקה, בבית משפט לחוקה ומתנגדים לפסקת התגברות.",
  "דת ומדינה": "תומכים בנישואים אזרחיים ובהעברת הסדרי שבת לרשויות המקומיות.",
  "חינוך": "דורשים לימודי ליבה כתנאי לתקצוב ותומכים בעצמאות ניהולית לבתי הספר.",
  "בריאות": "מציעים להוסיף כוח אדם ומיטות במערכת הציבורית ולהרחיב רפואה מונעת.",
  "מדיניות חוץ": "מעדיפים הסדר אזורי רחב ומסרבים למשא ומתן על מעמד ירושלים.",
  "עמדה קואליציונית": "קובעים שהממשלה הבאה תורכב רק ממפלגות שהמסמך מגדיר ציוניות.",
 },
 "shema": {
  "ביטחון": "מציעים חובת שירות לגברים יהודים, עם מסלול לחימה ומסלול לימוד ותפילה במסגרת הצבא.",
  "דיור": "מציעים סיוע ברכישת דירה לזוגות צעירים שסיימו שירות ומסלול לימודים.",
  "מערכת המשפט": "מבקשים לתת לממשלה הכרעה מחייבת במקרה של עימות בינה לבין בית המשפט העליון על חוק.",
  "דת ומדינה": "מתנגדים להכרה בנישואים חד מיניים ולמצעדי גאווה ומציעים חקיקה בתחומי המוסר המשפחתי.",
 },
 "otzma": {
  "חינוך": "תומכים בחיזוק לימודי הזהות והמסורת היהודית במוסדות החינוך.",
  "מדיניות חוץ": "תומכים בהחלת ריבונות ישראלית ביהודה ושומרון.",
 },
 "democrats": {
  "ביטחון": "תומכים בשירות לפי חוק ובהיפרדות מדינית מהפלסטינים עם ערבויות ביטחוניות.",
  "כלכלה ויוקר המחיה": "מציעים להגביר יבוא ותחרות בשוק המזון ולהגביל ריכוזיות.",
  "דיור": "מבקשים לשחרר קרקעות מדינה כדי להוזיל דיור.",
  "מערכת המשפט": "מבקשים לשמור על ביקורת שיפוטית ועצמאות בית המשפט ומתנגדים לפסקת התגברות גורפת.",
  "דת ומדינה": "תומכים בנישואים אזרחיים, בהכרה בגיורים מגוונים ובתחבורה מוגבלת בשבת באזורים מתאימים.",
  "חינוך": "תומכים במערכת חינוך ממלכתית אחת עם תוכנית ליבה וכללי תקצוב אחידים.",
  "בריאות": "מציעים להגדיל את כוח האדם והשירותים במערכת הבריאות הציבורית.",
  "מדיניות חוץ": "מתנגדים לסיפוח ותומכים בהרחבת הסכמי שלום אזוריים.",
  "עמדה קואליציונית": "שואפים להקים ממשלה ליברלית וציונית.",
 },
 "tzibur": {
  "ביטחון": "מבחינים בין מי שלימוד תורה הוא עיסוקו לבין אחרים, ותומכים בשירות מותאם לאחרים.",
  "כלכלה ויוקר המחיה": "מציעים הכשרה מקצועית ותעסוקה מתקדמת לציבור החרדי.",
  "דיור": "מבקשים להגדיל בנייה לציבור החרדי ולשלב שכונות חרדיות בערים שונות.",
  "דת ומדינה": "מבקשים לאפשר ללומדי תורה במשרה מלאה להמשיך ללמוד ללא מעצר או סנקציות.",
  "חינוך": "תומכים בעצמאות החינוך החרדי לצד חיזוק החינוך הממלכתי חרדי.",
 },
 "britolam": {
  "ביטחון": "דורשים ועדת חקירה ממלכתית לאירועי 7 באוקטובר.",
  "כלכלה ויוקר המחיה": "מציעים להפחית מסים בהדרגה, לבטל מע״מ על שירותים ולהחזיר נכסי מדינה שהופרטו.",
  "מערכת המשפט": "מציעים ועדה ממלכתית לניסוח חוקה ולבחינת שינויי מערכת המשפט ומינוי שופטים.",
  "מדיניות חוץ": "מציעים לבטל את הסכמי אוסלו ולהחיל ריבונות ישראלית ביהודה ושומרון ובעזה.",
 },
 "hatikun": {
  "ביטחון": "מציעים חובת שירות או תרומה אחרת לכל האזרחים, ואף התניה של זכות הבחירה במילוי חובה זו.",
  "מערכת המשפט": "תומכים בכינון חוקה הנשענת על שוויון, חירות ומגילת העצמאות.",
 },
 "gush": {
  "ביטחון": "מתנגדים לוויתורים טריטוריאליים וקוראים למאבק רחב בטרור.",
  "כלכלה ויוקר המחיה": "מבקשים למשוך השקעות מחוץ לישראל כדי לתמוך בכלכלה ובעלייה.",
  "בריאות": "מתנגדים לחובת התחסנות ומציגים את ההחלטה להתחסן כבחירה אישית.",
  "מדיניות חוץ": "מציעים גוש בין לאומי של מפלגות בעלות זהות יהודית נוצרית ומאמץ הסברה לישראל.",
 },
 "orot": {
  "ביטחון": "תומכים בעונש מוות לעוסקים בטרור ובחלוקת נטל השירות בדרך של הסכמות.",
  "כלכלה ויוקר המחיה": "מציעים מס בשיעור אחיד והפחתת רגולציה.",
  "דיור": "מבקשים לטפל ביוקר הדיור ובפעילות רשות מקרקעי ישראל.",
  "דת ומדינה": "תומכים בשימור מסורת יהודית ובהגדרת יהודי לפי ההלכה, תוך התנגדות לכפייה.",
  "חינוך": "תומכים בעצמאות חינוכית מקומית וקהילתית עם לימודי ליבה כלליים.",
  "מדיניות חוץ": "תומכים בריבונות ישראלית על כל שטחי הארץ ובעידוד הגירה מעזה.",
  "עמדה קואליציונית": "מציעים איחוד טכני של מפלגות אמוניות כדי למנוע אובדן קולות.",
 },
 "kachol": {
  "ביטחון": "תומכים בשירות צבאי, לאומי, אזרחי או קהילתי לכל המגזרים ובבלימת איומים מאיראן.",
  "כלכלה ויוקר המחיה": "מציעים כלכלה חופשית עם צדק חברתי, יותר תחרות ופתיחת שווקים ליבוא.",
  "מערכת המשפט": "מדגישים עצמאות משפטית וחוק יסוד החקיקה בהסכמה רחבה, לצד הגבלת כהונת ראש ממשלה.",
  "דת ומדינה": "מבקשים לתת לשלטון המקומי יותר סמכות בעיצוב המרחב הציבורי בשבת.",
  "חינוך": "תומכים בחיזוק החינוך הציבורי ומעמד המורה.",
  "בריאות": "מבקשים להגדיל את מספר אנשי הרפואה והסיעוד במערכת הציבורית.",
  "מדיניות חוץ": "משלבים תפיסת ביטחון תקיפה עם חיזוק הסכמי שלום וצמצום הסכסוך.",
  "עמדה קואליציונית": "מבקשים ממשלת אחדות רחבה ויציבה.",
 },
 "noam": {
  "מערכת המשפט": "תומכים בפסקת התגברות ובפיצול תפקיד היועץ המשפטי לממשלה.",
  "דת ומדינה": "תומכים בשבת כיום מנוחה ציבורי ובחיזוק הרבנות הראשית.",
  "חינוך": "מציעים בחירה רחבה יותר בבתי ספר, כיתות קטנות וחיזוק לימודי מורשת יהודית.",
 },
 "tzomet": {
  "שירות לכול": "בית ישראל, השותפה ברשימה, מציעה שירות לאומי או אזרחי לכול, תגמול למשרתים וסנקציות למשתמטים.",
  "התיישבות ופיתוח": "בית ישראל מציעה תוכנית ארוכת טווח לפיתוח תשתיות, ערים והתיישבות.",
  "עלייה וקליטה": "בית ישראל מציעה להשלים את העלייה מאתיופיה ולבנות מדיניות קליטה קבועה.",
  "פנסיה": "בית ישראל מציעה השלמת פנסיה עד שכר מינימום למי שעבד ושירת.",
  "ביטחון אישי": "בית ישראל מציעה רפורמה באכיפת החוק והרחקת עבריינים סדרתיים.",
 },
}

# id, topic in the research profile, Hebrew, English, Arabic, Russian,
# documented supporters, documented opponents. Do not infer stances from the
# party's name or from ideological proximity.
ADDITIONS = [
 ("core-curriculum","חינוך","האם יש להתנות תקצוב בתי ספר בלימודי ליבה מלאים?","Should public funding of schools require a full core curriculum?","هل يجب ربط تمويل المدارس بتدريس المنهج الأساسي كاملًا؟","Следует ли выделять государственное финансирование школам только при преподавании полной базовой программы?","beyachad yashar beytenu democrats",""),
 ("school-vouchers","חינוך","האם יש לתת להורים שובר חינוך כדי לבחור את בית הספר של ילדיהם?","Should parents receive education vouchers to choose their children's school?","هل يجب منح الأهل قسائم تعليمية لاختيار مدرسة أطفالهم؟","Следует ли выдавать родителям образовательные ваучеры для выбора школы?","haskel",""),
 ("school-autonomy","חינוך","האם יש להעביר יותר סמכויות בניהול בתי הספר לרשויות המקומיות ולמנהלים?","Should local authorities and principals have more control over schools?","هل يجب نقل مزيد من صلاحيات إدارة المدارس إلى السلطات المحلية والمديرين؟","Следует ли передать больше полномочий по управлению школами муниципалитетам и директорам?","beyachad yashar beytenu orot",""),
 ("territorial-concessions","ביטחון","האם ישראל צריכה להימנע מוויתורים טריטוריאליים בהסדרים עתידיים?","Should Israel avoid territorial concessions in future agreements?","هل يجب أن تمتنع إسرائيل عن التنازلات الإقليمية في الاتفاقات المستقبلية؟","Следует ли Израилю отказаться от территориальных уступок в будущих соглашениях?","gush",""),
 ("teacher-pay","חינוך","האם יש להעלות את שכר המורים כדי לחזק את מערכת החינוך?","Should teachers' pay be raised to strengthen education?","هل يجب رفع رواتب المعلمين لتعزيز التعليم؟","Следует ли повысить зарплаты учителей для укрепления образования?","beyachad ganeden noam",""),
 ("early-childcare","חינוך","האם יש להרחיב את ההשקעה הציבורית במסגרות לגיל הרך?","Should public investment in early childhood care be expanded?","هل يجب توسيع الاستثمار العام في رعاية الطفولة المبكرة؟","Следует ли увеличить государственные инвестиции в уход за детьми раннего возраста?","beyachad beytenu",""),
 ("public-healthcare","בריאות","האם יש להרחיב את שירותי הבריאות הציבוריים ולהוסיף כוח אדם רפואי?","Should public healthcare be expanded with more medical staff?","هل يجب توسيع خدمات الصحة العامة وزيادة الطواقم الطبية؟","Следует ли расширить государственное здравоохранение и увеличить медицинский персонал?","beytenu democrats kachol",""),
 ("preventive-care","בריאות","האם יש להגדיל את ההשקעה ברפואה מונעת?","Should investment in preventive healthcare increase?","هل يجب زيادة الاستثمار في الطب الوقائي؟","Следует ли увеличить инвестиции в профилактическую медицину?","beyachad ganeden beytenu",""),
 ("trauma-care","בריאות","האם יש להקים תוכנית לאומית לטיפול בטראומה ובחוסן נפשי?","Should Israel establish a national trauma and mental resilience program?","هل يجب إنشاء برنامج وطني لعلاج الصدمات وتعزيز الصمود النفسي؟","Следует ли создать национальную программу помощи при травмах и укрепления психологической устойчивости?","yashar ganeden",""),
 ("long-term-rent","דיור","האם יש להרחיב את ההיצע של דירות להשכרה ארוכת טווח?","Should the supply of long-term rental housing be expanded?","هل يجب توسيع المعروض من المساكن للإيجار طويل الأمد؟","Следует ли расширить предложение жилья для долгосрочной аренды?","beyachad beytenu",""),
 ("reservist-housing","דיור","האם יש לתת למשרתי מילואים עדיפות או סיוע מיוחד בדיור?","Should reservists receive housing priority or special assistance?","هل يجب منح جنود الاحتياط أولوية أو مساعدة خاصة في السكن؟","Следует ли предоставлять резервистам приоритет или особую помощь с жильем?","beyachad yashar",""),
 ("participatory-voting","שיטת ממשל","האם יש לאפשר לבוחרים להצביע ישירות על החלטות או להאציל את קולם באופן גמיש?","Should voters be able to vote directly on decisions or delegate their vote flexibly?","هل يجب تمكين الناخبين من التصويت المباشر على القرارات أو تفويض أصواتهم بمرونة؟","Следует ли позволить избирателям голосовать по решениям напрямую или гибко делегировать голос?","pirates",""),
 ("food-monopolies","כלכלה ויוקר המחיה","האם יש לפרק מונופולים ולהגביר תחרות כדי להוזיל מזון?","Should monopolies be broken up and competition increased to lower food prices?","هل يجب تفكيك الاحتكارات وتعزيز المنافسة لخفض أسعار الغذاء؟","Следует ли бороться с монополиями и усилить конкуренцию ради снижения цен на продукты?","beyachad yashar beytenu democrats",""),
 ("business-regulation","כלכלה ויוקר המחיה","האם יש להפחית רגולציה על עסקים כחלק מהמאבק ביוקר המחיה?","Should business regulation be reduced to tackle the cost of living?","هل يجب تقليص تنظيم الأعمال لمواجهة غلاء المعيشة؟","Следует ли уменьшить регулирование бизнеса для снижения стоимости жизни?","beytenu orot",""),
 ("torah-study-protection","דת ומדינה","האם יש לאפשר למי שלימוד תורה הוא עיסוקו להמשיך ללמוד ללא סנקציות גיוס?","Should full-time Torah students be allowed to continue studying without draft sanctions?","هل يجب السماح للمتفرغين لدراسة التوراة بمواصلة الدراسة دون عقوبات التجنيد؟","Следует ли разрешить постоянно изучающим Тору продолжать учёбу без санкций за непризыв?","tzibur",""),
 ("services-vat","כלכלה ויוקר המחיה","האם יש לבטל את המע״מ על שירותים?","Should VAT on services be abolished?","هل يجب إلغاء ضريبة القيمة المضافة على الخدمات؟","Следует ли отменить НДС на услуги?","britolam",""),
 ("debt-relief","כלכלה ויוקר המחיה","האם יש לקדם תוכנית רחבה למחיקת חובות של אזרחים?","Should a broad program to cancel citizens' debts be introduced?","هل يجب إطلاق برنامج واسع لإسقاط ديون المواطنين؟","Следует ли принять широкую программу списания долгов граждан?","ganeden",""),
 ("basic-income","כלכלה ויוקר המחיה","האם יש לשלם לכל אזרח שכר בסיס מטעם המדינה?","Should the state provide every citizen with a basic income?","هل يجب أن توفر الدولة دخلًا أساسيًا لكل مواطن؟","Следует ли государству выплачивать каждому гражданину базовый доход?","ganeden",""),
 ("fewer-ministries","כלכלה ויוקר המחיה","האם יש לצמצם או לאחד משרדי ממשלה?","Should government ministries be reduced or merged?","هل يجب تقليص الوزارات الحكومية أو دمجها؟","Следует ли сократить число министерств или объединить их?","yashar haskel beytenu",""),
 ("written-constitution","מערכת המשפט","האם ישראל צריכה לחוקק חוקה כתובה המגדירה את סמכויות הרשויות וזכויות האזרחים?","Should Israel adopt a written constitution defining government powers and civil rights?","هل يجب أن تعتمد إسرائيل دستورًا مكتوبًا يحدد صلاحيات السلطات وحقوق المواطنين؟","Следует ли Израилю принять письменную конституцию, определяющую полномочия властей и права граждан?","beyachad yashar beytenu britolam hatikun",""),
 ("judicial-independence","מערכת המשפט","האם יש לשמור על עצמאות מערכת המשפט וביקורת שיפוטית על השלטון?","Should judicial independence and review of government actions be protected?","هل يجب حماية استقلال القضاء والرقابة القضائية على السلطة؟","Следует ли защищать независимость суда и судебный контроль над властью?","yashar beytenu democrats kachol",""),
 ("override-clause","מערכת המשפט","האם יש לאפשר לכנסת להתגבר בחקיקה על החלטות בג״ץ?","Should the Knesset be able to override Supreme Court rulings by legislation?","هل يجب تمكين الكنيست من تجاوز أحكام المحكمة العليا بتشريع؟","Следует ли разрешить Кнессету преодолевать решения Верховного суда законом?","noam","beytenu democrats"),
 ("local-shabbat","דת ומדינה","האם כל רשות מקומית צריכה לקבוע בעצמה את הסדרי התחבורה הציבורית בשבת?","Should each local authority decide its own Shabbat public transport rules?","هل يجب أن تحدد كل سلطة محلية بنفسها ترتيبات النقل العام يوم السبت؟","Следует ли каждому муниципалитету самому определять правила общественного транспорта в шаббат?","beyachad beytenu kachol",""),
 ("civil-marriage","דת ומדינה","האם יש לאפשר נישואים או ברית זוגיות אזרחית בישראל?","Should civil marriage or civil unions be available in Israel?","هل يجب إتاحة الزواج أو الشراكة المدنية في إسرائيل؟","Следует ли разрешить гражданский брак или гражданский союз в Израиле?","beyachad beytenu democrats",""),
 ("pluralist-conversion","דת ומדינה","האם יש להכיר בגיורים ובמסלולי כשרות נוספים מחוץ לרבנות הראשית?","Should conversion and kosher certification outside the Chief Rabbinate be recognized?","هل يجب الاعتراف بعمليات تهويد وشهادات كوشير خارج الحاخامية الرئيسية؟","Следует ли признавать гиюр и кашрут вне Главного раввината?","beyachad beytenu democrats",""),
 ("jewish-identity-education","חינוך","האם יש לחזק לימודי יהדות ומסורת במערכת החינוך?","Should Jewish heritage studies be strengthened in schools?","هل يجب تعزيز دراسة التراث والتقاليد اليهودية في المدارس؟","Следует ли усилить изучение еврейской традиции в школах?","otzma noam",""),
 ("service-alternatives","ביטחון","האם יש לאפשר שירות אזרחי או לאומי כחלופה לשירות צבאי?","Should civilian or national service be available as an alternative to military service?","هل يجب إتاحة الخدمة المدنية أو الوطنية بديلًا عن الخدمة العسكرية؟","Следует ли разрешить гражданскую или национальную службу вместо военной?","yashar beytenu kachol haskel hatikun shema",""),
 ("professional-army","ביטחון","האם יש לבטל גיוס חובה ולעבור לצבא מקצועי של מתנדבים?","Should conscription end in favor of a professional volunteer army?","هل يجب إلغاء التجنيد الإلزامي والانتقال إلى جيش مهني تطوعي؟","Следует ли отменить обязательный призыв и перейти к профессиональной добровольческой армии?","byn","beyachad beytenu democrats"),
 ("voting-service","מערכת המשפט","האם יש להתנות את זכות ההצבעה במילוי חובת שירות או תרומה למדינה?","Should voting rights depend on completing service or another contribution to the state?","هل يجب ربط حق التصويت بأداء الخدمة أو مساهمة أخرى للدولة؟","Следует ли ставить право голоса в зависимость от службы или иного вклада в государство?","hatikun",""),
 ("arab-community-crime","ביטחון","האם יש להגביר אכיפה ושיתוף פעולה נגד הפשיעה ביישובים ערביים?","Should enforcement and cooperation against crime in Arab communities increase?","هل يجب تعزيز إنفاذ القانون والتعاون ضد الجريمة في البلدات العربية؟","Следует ли усилить борьбу с преступностью и сотрудничество в арабских населенных пунктах?","byn",""),
 ("cancel-oslo","מדיניות חוץ","האם יש לבטל רשמית את הסכמי אוסלו?","Should the Oslo Accords be formally cancelled?","هل يجب إلغاء اتفاقيات أوسلو رسميًا؟","Следует ли официально отменить соглашения Осло?","haskel britolam",""),
 ("west-bank-sovereignty","מדיניות חוץ","האם יש להחיל ריבונות ישראלית על יהודה ושומרון?","Should Israel apply its sovereignty to the West Bank?","هل يجب أن تفرض إسرائيل سيادتها على الضفة الغربية؟","Следует ли Израилю распространить свой суверенитет на Западный берег?","otzma britolam orot","democrats"),
]

# These official documents address four of the original policy questions but
# were absent from the pinned general-purpose issue dataset.
SUPPLEMENTAL_POSITIONS = [
    ("shema", "lgbt-equality", -2, "דת ומדינה"),
    ("tzibur", "haredi-draft", -2, "דת ומדינה"),
    ("byn", "universal-service", -2, "ביטחון"),
    ("britolam", "oct7-inquiry", 2, "ביטחון"),
]


def main() -> None:
    data = json.loads(CONTENT.read_text(encoding="utf-8"))
    if data["revision"] == 2 and len(data["questions"]) == 50:
        original_ids = {q["id"] for q in data["questions"][:18]}
        supplemental_keys = {(party_id, qid) for party_id, qid, _, _ in SUPPLEMENTAL_POSITIONS}
        data["questions"] = data["questions"][:18]
        data["positions"] = [p for p in data["positions"] if p["questionId"] in original_ids and (p["listId"], p["questionId"]) not in supplemental_keys]
        data.pop("profiles", None)
        data["revision"] = 1
    assert data["revision"] == 1 and len(data["questions"]) == 18
    sources = json.loads(SOURCES.read_text(encoding="utf-8"))
    assert len(sources) == len(PARTY_IDS) == 38
    assert len(ADDITIONS) == 32
    assert set(PROFILE_POINTS) <= set(PARTY_IDS)
    assert all(set(topics) <= set(sources[party_id]["sources"]) for party_id, topics in PROFILE_POINTS.items())
    names = {item["id"]: item["he"] for item in data["lists"]}
    data["profiles"] = [
        {"listId": party_id, "sourceIndex": source["page"],
         "bullets": [{"topic": topic, "summary": summary,
                      "sourceUrl": source["sources"][topic]}
                     for topic, summary in PROFILE_POINTS.get(party_id, {}).items()]}
        for party_id, source in sources.items()
    ]
    existing = {(p["listId"], p["questionId"]) for p in data["positions"]}
    for qid, topic, he, en, ar, ru, yes, no in ADDITIONS:
        party_ids = (yes + " " + no).split()
        assert party_ids and len(party_ids) == len(set(party_ids))
        source_party = sources[party_ids[0]]
        data["questions"].append({"id": qid, "topicHe": topic, "he": he,
                                  "en": en, "ar": ar, "ru": ru,
                                  "sourceUrl": "https://elections.handled.team/topics/"})
        for party_id in party_ids:
            item = sources[party_id]["sources"][topic]
            key = party_id, qid
            assert key not in existing
            existing.add(key)
            data["positions"].append({
                "listId": party_id, "questionId": qid,
                "value": 2 if party_id in yes.split() else -2,
                "sourceType": "official_platform",
                "sourceTitle": f"Official platform: {names[party_id]}",
                "sourceDate": "", "sourceUrl": item,
                "verifiedAt": "2026-10-03",
            })
    for party_id, qid, value, topic in SUPPLEMENTAL_POSITIONS:
        key = party_id, qid
        assert key not in existing
        existing.add(key)
        data["positions"].append({
            "listId": party_id, "questionId": qid, "value": value,
            "sourceType": "official_platform",
            "sourceTitle": f"Official platform: {names[party_id]}",
            "sourceDate": "", "sourceUrl": sources[party_id]["sources"][topic],
            "verifiedAt": "2026-10-03",
        })
    # Lists without a published platform can still have directly sourced
    # positions in the original dataset. Show a few exact question/stance
    # pairs as profile highlights, without inventing a wider party principle.
    questions_by_id = {q["id"]: q for q in data["questions"]}
    highlights = [
        "palestinian-state", "judicial-reform", "religion-state",
        "lower-taxes", "lgbt-equality", "haredi-draft", "oct7-inquiry",
        "food-imports", "settlements", "universal-service",
    ] + [q["id"] for q in data["questions"][:18]]
    profile_by_id = {profile["listId"]: profile for profile in data["profiles"]}
    for party_id, profile in profile_by_id.items():
        if profile["bullets"]:
            continue
        positions_by_question = {p["questionId"]: p for p in data["positions"] if p["listId"] == party_id and p["questionId"] in questions_by_id and p["questionId"] in {q["id"] for q in data["questions"][:18]}}
        selected = list(dict.fromkeys(highlights))
        for qid in selected:
            if qid not in positions_by_question:
                continue
            position = positions_by_question[qid]
            stance = {2: "בעד ההצעה", 0: "עמדה חלקית או מותנית", -2: "נגד ההצעה"}[position["value"]]
            question = questions_by_id[qid]
            profile["bullets"].append({
                "topic": question["topicHe"],
                "summary": f"{stance} בשאלה: {question['he']}",
                "sourceUrl": position["sourceUrl"],
            })
            if len(profile["bullets"]) == 3:
                break
    data["revision"] = 2
    data["updatedAt"] = "2026-10-03"
    data["sourceVersion"] = "2026-10-03-platform-review"
    data["attribution"]["platformIndex"] = "https://elections.handled.team/"
    data["attribution"].pop("platformLicense", None)
    data["attribution"]["adaptation"] = "Original 18 questions and documented positions retained. 32 new questions and original source-linked platform summaries added from official documents indexed by elections.handled.team on 2026-10-03. Unknown positions remain unknown."
    assert len(data["questions"]) == 50
    assert len({q["id"] for q in data["questions"]}) == 50
    assert len(data["positions"]) == len(existing)
    CONTENT.write_bytes((json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8"))
    print(f"Wrote {len(data['questions'])} questions, {len(data['lists'])} lists, {len(data['positions'])} sourced positions, {sum(len(p['bullets']) for p in data['profiles'])} profile bullets")


if __name__ == "__main__":
    main()
