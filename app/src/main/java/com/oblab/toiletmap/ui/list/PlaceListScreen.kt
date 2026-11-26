package com.oblab.toiletmap.ui.list

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oblab.toiletmap.data.formatDistance
import com.oblab.toiletmap.ui.theme.DividerColor
import com.oblab.toiletmap.ui.theme.Fill
import com.oblab.toiletmap.ui.theme.Green
import com.oblab.toiletmap.ui.theme.GreenDark
import com.oblab.toiletmap.ui.theme.GreenTint
import com.oblab.toiletmap.ui.theme.Ink
import com.oblab.toiletmap.ui.theme.SubText

data class PlaceListItem(
    val id: String,
    val name: String?,
    val address: String?,
    /** 내 위치를 모르면 null */
    val distanceMeters: Int?,
    val lastVisitedAt: Long,
    val visitCount: Int,
)

enum class PlaceSort { RECENT, NEAREST }

/** 이름·주소에 [query]가 들어간 장소만 남기고 [sort] 순서로 정렬. */
internal fun filterAndSort(places: List<PlaceListItem>, query: String, sort: PlaceSort): List<PlaceListItem> {
    val q = query.trim()
    val found = if (q.isEmpty()) places else places.filter {
        it.name?.contains(q, ignoreCase = true) == true || it.address?.contains(q, ignoreCase = true) == true
    }
    return when (sort) {
        PlaceSort.RECENT -> found.sortedByDescending { it.lastVisitedAt }
        PlaceSort.NEAREST -> found.sortedBy { it.distanceMeters ?: Int.MAX_VALUE }
    }
}

/** 목록/검색 화면. 검색어가 있으면 정렬 대신 검색 결과 수를 보여준다. */
@Composable
fun PlaceListScreen(
    places: List<PlaceListItem>,
    onBack: () -> Unit = {},
    onPlaceClick: (PlaceListItem) -> Unit = {},
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var sort by rememberSaveable { mutableStateOf(PlaceSort.RECENT) }
    val q = query.trim()
    val shown = remember(places, q, sort) { filterAndSort(places, q, sort) }

    Column(
        Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "지도로 돌아가기", tint = Ink)
            }
            Text("내 화장실", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (q.isEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text("${places.size}곳", fontSize = 15.sp, color = SubText)
            }
        }

        SearchField(query) { query = it }

        if (q.isEmpty()) {
            SortToggle(sort) { sort = it }
        } else {
            Text("검색 결과 ${shown.size}곳 · 이름과 주소에서 찾음", fontSize = 13.sp, color = SubText)
        }

        if (q.isNotEmpty() && shown.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("'$q' 검색 결과가 없습니다", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink, textAlign = TextAlign.Center)
                Text("이름을 지정하지 않은 장소는 주소로 찾아보십시오", fontSize = 13.sp, color = SubText, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn {
                items(shown, key = { it.id }) { place -> PlaceRow(place, q) { onPlaceClick(place) } }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val active = query.isNotEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (active) Color.White else Fill, CircleShape)
            .border(2.dp, if (active) Green else Color.Transparent, CircleShape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = if (active) Green else SubText)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).semantics { contentDescription = "장소 검색" },
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = Ink),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("이름·주소 검색", color = SubText, fontSize = 15.sp)
                    inner()
                }
            },
        )
        if (active) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "검색어 지우기", tint = SubText)
            }
        }
    }
}

@Composable
private fun SortToggle(sort: PlaceSort, onChange: (PlaceSort) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Fill, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(PlaceSort.RECENT to "최근 방문순", PlaceSort.NEAREST to "가까운 순").forEach { (value, label) ->
            val on = sort == value
            Surface(
                selected = on,
                onClick = { onChange(value) },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (on) Color.White else Color.Transparent,
                shadowElevation = if (on) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) Ink else Color(0xFF4A514C),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceListItem, query: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).background(GreenTint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (place.name != null) {
                    Text(highlight(place.name, query), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                } else {
                    Text("이름 없음", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SubText)
                }
                Text(
                    place.address?.let { highlight(it, query) } ?: AnnotatedString("주소 확인 중"),
                    fontSize = 13.sp,
                    color = SubText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                place.distanceMeters?.let {
                    Text(formatDistance(it.toDouble()), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                }
                Text(
                    "${DateUtils.getRelativeTimeSpanString(place.lastVisitedAt)} · ${place.visitCount}회",
                    fontSize = 12.sp,
                    color = SubText,
                )
            }
        }
        HorizontalDivider(color = DividerColor)
    }
}

/** [query]와 처음 일치하는 부분만 강조. */
private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    append(text)
    val start = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (start >= 0) addStyle(SpanStyle(background = GreenTint, color = GreenDark), start, start + query.length)
}


private val PreviewPlaces: List<PlaceListItem> = run {
    val now = System.currentTimeMillis()
    val day = DateUtils.DAY_IN_MILLIS
    listOf(
        PlaceListItem("1", "역삼 센터빌딩 2층", "서울 강남구 테헤란로 [번지]", 120, now - 2 * day, 3),
        PlaceListItem("2", null, null, 650, now - day, 1),
        PlaceListItem("3", "선릉 공원 공중화장실", "서울 강남구 선릉로 [번지]", 480, now - 5 * day, 1),
        PlaceListItem("4", "삼성역 지하상가", "서울 강남구 영동대로 [번지]", 1200, now - 7 * day, 5),
        PlaceListItem("5", "판교 오피스 1층", "경기 성남시 분당구 [번지]", 14000, now - 14 * day, 2),
        PlaceListItem("6", null, "서울 강남구 역삼동 [번지]", 650, now - 3 * day, 1),
    )
}

@Preview(name = "목록 · 최근 방문순", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun PlaceListPreview() {
    MaterialTheme { PlaceListScreen(PreviewPlaces) }
}

@Preview(name = "목록 · 검색 결과", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun PlaceListSearchPreview() {
    MaterialTheme { PlaceListScreen(PreviewPlaces, initialQuery = "역삼") }
}

@Preview(name = "목록 · 검색 결과 없음", widthDp = 390, heightDp = 844, locale = "ko")
@Composable
private fun PlaceListEmptyPreview() {
    MaterialTheme { PlaceListScreen(PreviewPlaces, initialQuery = "역삼역 3번") }
}
