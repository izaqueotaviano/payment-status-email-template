package com.iptvtv.player.ui.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.ui.components.dpadFocusEscape
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandPrimary
import com.iptvtv.player.ui.theme.BrandSurfaceVariant

/**
 * Labeled text input for D-pad friendly forms. Compose for TV has no TextField component, so
 * this wraps a [BasicTextField] with a label and a border that lights up when focused.
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(12.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = BrandMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = BrandOnSurface, fontSize = 15.sp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(BrandPrimary),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .dpadFocusEscape(focusManager)
                .background(BrandSurfaceVariant, shape)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) BrandPrimary else BrandOutline,
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}
