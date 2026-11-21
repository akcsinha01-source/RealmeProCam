package com.realme.procam.ui.guide

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.camera.RealmeCameraConstants.CameraMode
import com.realme.procam.theme.*

enum class GuideCategory(val titleHindi: String, val titleEnglish: String) {
    DUAL_FOCUS("हाइब्रिड फोकस स्टैकिंग", "Dual-Lens Stacking"),
    PRO_CONTROLS("प्रो मैनुअल कंट्रोल्स", "Pro Manual"),
    COMPUTATIONAL("नाइट और AI मोड्स", "Night & AI Modes"),
    VIEWFINDER_TOOLS("व्यूफाइंडर टूल्स", "Cinematography Tools")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureGuideScreen(
    onDismiss: () -> Unit,
    onLaunchMode: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(GuideCategory.DUAL_FOCUS) }
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Realme 14 Pro+ प्रो कैमरा गाइड",
                            color = RealmeGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "सीखें और तुरंत आज़माएँ (Interactive Demos)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledSurface)
            )
        },
        containerColor = Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Tabs Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledSurface)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GuideCategory.values()) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RealmeGold.copy(alpha = 0.22f) else OledCard)
                            .border(1.dp, if (isSelected) RealmeGold else OledBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedCategory = cat
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = cat.titleHindi,
                                color = if (isSelected) RealmeGold else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = cat.titleEnglish,
                                color = if (isSelected) RealmeGold.copy(alpha = 0.8f) else TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Category Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedCategory) {
                    GuideCategory.DUAL_FOCUS -> {
                        item {
                            DualFocusGuideSection(
                                onTryNow = {
                                    onLaunchMode(CameraMode.DUAL_FOCUS)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    GuideCategory.PRO_CONTROLS -> {
                        item {
                            ProControlsGuideSection(
                                onTryNow = {
                                    onLaunchMode(CameraMode.PRO)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    GuideCategory.COMPUTATIONAL -> {
                        item {
                            ComputationalModesGuideSection(
                                onLaunchMode = { mode ->
                                    onLaunchMode(mode)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    GuideCategory.VIEWFINDER_TOOLS -> {
                        item {
                            ViewfinderToolsGuideSection(
                                onTryNow = {
                                    onLaunchMode(CameraMode.PRO)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. Hybrid Dual-Lens Focus Stacking Guide Section
 */
@Composable
private fun DualFocusGuideSection(onTryNow: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "हाइब्रिड डुअल-लेंस फोकस स्टैकिंग क्या है?",
            color = RealmeGold,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "यह तकनीक Realme 14 Pro+ के Main Wide (1x) और Periscope Telephoto (3x) दोनों कैमरों को एक साथ जोड़ती है। पास की वस्तु और दूर का बैकग्राउंड दोनों एक ही तस्वीर में 100% क्रिस्टल क्लियर दिखते हैं!",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Before / After Slider
        Text(
            text = "लाइव तुलना (Interactive Before / After Demo):",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        BeforeAfterSlider()

        Spacer(modifier = Modifier.height(18.dp))

        // 3-Step Walkthrough Cards
        Text(
            text = "सरल 3-स्टेप उपयोग विधि:",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        StepCard(
            stepNum = "1",
            titleHindi = "स्टेप 1: नज़दीकी विषय (Main 1x)",
            description = "मेन 1x लेंस सक्रिय रहेगा। पास के ऑब्जेक्ट (फूल, घड़ी, व्यक्ति) पर टैप करके फोकस करें और पहली फोटो लें।"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StepCard(
            stepNum = "2",
            titleHindi = "स्टेप 2: घोस्ट ओवरले अलाइनमेंट (3x Periscope)",
            description = "कैमरा अपने आप 3x पेरिस्कोप लेंस पर स्विच करेगा। स्क्रीन पर पहली फोटो की हल्की परछाई (Ghost Overlay) दिखेगी, जिससे दूर के बैकग्राउंड को मिलाएँ।"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StepCard(
            stepNum = "3",
            titleHindi = "स्टेप 3: दूर का बैकग्राउंड और AI ब्लेंडिंग",
            description = "दूर वाले विषय पर टैप करें। AI दोनों फोटो को स्केल और लैप्लासियन पिरामिड द्वारा मिलाकर एक अल्ट्रा-शार्प मास्टर फोटो तैयार करेगा!"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Try Now Button
        TryFeatureButton(text = "अभी आज़माएं: हाइब्रिड फोकस स्टैक मोड", onClick = onTryNow)
    }
}

/**
 * 2. Pro Manual Controls Guide Section
 */
@Composable
private fun ProControlsGuideSection(onTryNow: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "प्रो मैनुअल कंट्रोल्स (DSLR-Grade Controls)",
            color = RealmeGold,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ISO, शटर स्पीड (1/8000s से 32s), और मैनुअल फोकस को अपनी पसंद अनुसार सेट करें। नीचे दिए गए इंटरैक्टिव सिमुलेटर पर खुद हाथ आज़माएँ:",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        SimulatedIsoDemo()
        Spacer(modifier = Modifier.height(12.dp))
        SimulatedShutterDemo()
        Spacer(modifier = Modifier.height(12.dp))
        SimulatedFocusPeakingDemo()

        Spacer(modifier = Modifier.height(18.dp))
        TryFeatureButton(text = "अभी आज़माएं: प्रो मैनुअल कैमरा", onClick = onTryNow)
    }
}

/**
 * 3. Night & AI Computational Modes Guide Section
 */
@Composable
private fun ComputationalModesGuideSection(onLaunchMode: (CameraMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "AI और कम्प्यूटेशनल फोटोग्राफी मोड्स",
            color = RealmeGold,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        FeatureTile(
            title = "Astro Starry Sky (तारे और रात का आकाश)",
            description = "मल्टी-फ्रेम स्टार अलाइनमेंट और डार्क-फ्रेम नॉइज़ रिडक्शन से रात के आसमान में आकाशगंगा और तारे साफ चमकते हैं।",
            icon = Icons.Default.NightsStay,
            onTry = { onLaunchMode(CameraMode.ASTRO) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureTile(
            title = "Long Exposure / Light Trails (रोशनी की लकीरें)",
            description = "बिना ट्राइपॉड के गाड़ियों की हेडलाइट की लंबी लकीरें और झरने का रेशमी पानी (Silky Water) कैप्चर करें।",
            icon = Icons.Default.AllInclusive,
            onTry = { onLaunchMode(CameraMode.LONG_EXPOSURE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureTile(
            title = "Zero-Shutter-Lag HDR+ (ब्रैकेटेड डायनामिक रेंज)",
            description = "-2 EV, 0 EV, और +2 EV की तीन तस्वीरें एक साथ लेकर सूरज की रोशनी और गहरी परछाइयों को संतुलित करता है।",
            icon = Icons.Default.HdrOn,
            onTry = { onLaunchMode(CameraMode.HDR_PLUS) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureTile(
            title = "Super Macro (3x पेरिस्कोप मैक्रो)",
            description = "पेरिस्कोप लेंस के मिनिमम फोकस डिस्टैन्स और फोकस पीकिंग का उपयोग करके नन्हीं वस्तुओं के सूक्ष्म विवरण कैद करें।",
            icon = Icons.Default.Search,
            onTry = { onLaunchMode(CameraMode.SUPER_MACRO) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureTile(
            title = "Document / Map Scanner (दस्तावेज़ स्कैनर)",
            description = "किनारों को अपने आप पहचानकर सीधा करता है और हाई-कंट्रास्ट ब्लैक एंड व्हाइट एनहांसमेंट देता है।",
            icon = Icons.Default.DocumentScanner,
            onTry = { onLaunchMode(CameraMode.SCANNER) }
        )
    }
}

/**
 * 4. Cinematography Viewfinder Tools Guide Section
 */
@Composable
private fun ViewfinderToolsGuideSection(onTryNow: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "सिनेमैटोग्राफी और व्यूफाइंडर टूल्स",
            color = RealmeGold,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        ToolExplainCard(
            title = "इलेक्ट्रॉनिक होराइजन लेवल (Electronic Horizon Level)",
            desc = "फोन का जाइरोस्कोप रियल-टाइम झुकाव नापता है। जब फोन बिल्कुल सीधा (0°) होता है, तो लाइन हरी हो जाती है और एक हल्का हैप्टिक कंपन मिलता है।"
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolExplainCard(
            title = "लाइव हिस्टोग्राम (RGB & Luma Histogram)",
            desc = "स्क्रीन के कोने में रियल-टाइम ग्राफ दिखाता है कि रोशनी बहुत कम (अंडरएक्सपोज्ड) या बहुत ज़्यादा (ओवरएक्सपोज्ड) तो नहीं हो रही।"
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolExplainCard(
            title = "जेब्रा स्ट्राइप्स (Zebra Stripes Overexposure Alert)",
            desc = "फोटो के जिन हिस्सों में रोशनी 95% से अधिक हो जाएगी, वहाँ काली-सफेद तिरछी रेखाएं चेतावनी देंगी।"
        )
        Spacer(modifier = Modifier.height(8.dp))
        ToolExplainCard(
            title = "फ्रेमिंग ग्रिड्स (Rule of Thirds & Golden Ratio)",
            desc = "तस्वीर की सही बनावट के लिए रूल ऑफ थर्ड्स और गोल्डन स्पाइरल ग्रिड्स।"
        )

        Spacer(modifier = Modifier.height(18.dp))
        TryFeatureButton(text = "अभी आज़माएं: प्रो व्यूफाइंडर", onClick = onTryNow)
    }
}

@Composable
private fun StepCard(stepNum: String, titleHindi: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OledCard)
            .border(1.dp, OledBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(RealmeGold.copy(alpha = 0.2f))
                .border(1.dp, RealmeGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNum, color = RealmeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titleHindi, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun FeatureTile(title: String, description: String, icon: ImageVector, onTry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OledCard)
            .border(1.dp, OledBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onTry)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = RealmeGold, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ToolExplainCard(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(OledCard)
            .border(1.dp, OledBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(text = title, color = RealmeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun TryFeatureButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RealmeGold),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
