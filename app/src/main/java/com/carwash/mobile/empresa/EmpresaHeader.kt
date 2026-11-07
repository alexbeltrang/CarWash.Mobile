package com.carwash.mobile.empresa

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R

val LocalEmpresa = compositionLocalOf<EmpresaDto?> { null }
val LocalEmpresaRefresh = compositionLocalOf<() -> Unit> { {} }

@Composable
fun EmpresaHeader(title: String) {
    val company = LocalEmpresa.current
    if (company == null) { Text(title); return }
    val bitmap = remember(company.logoBase64) {
        runCatching { company.logoBase64?.let { encoded ->
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } }.getOrNull()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (bitmap != null) {
            Image(bitmap, stringResource(R.string.company_logo), Modifier.size(36.dp).testTag("companyLogo"), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(company.nombreComercial, Modifier.testTag("companyName"), style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}