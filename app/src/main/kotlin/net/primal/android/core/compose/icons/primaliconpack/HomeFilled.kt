package net.primal.android.core.compose.icons.primaliconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import net.primal.android.core.compose.icons.PrimalIcons

public val PrimalIcons.HomeFilled: ImageVector
    get() {
        if (_homefilled != null) {
            return _homefilled!!
        }
        _homefilled = Builder(name = "Homefilled", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFFffffff)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = EvenOdd) {
                moveTo(23.5881f, 9.4787f)
                lineTo(12.6018f, 1.2013f)
                curveTo(12.2455f, 0.9329f, 11.7545f, 0.9329f, 11.3982f, 1.2013f)
                lineTo(0.4119f, 9.4787f)
                curveTo(-0.0348f, 9.8152f, -0.1335f, 10.4631f, 0.1913f, 10.9259f)
                curveTo(0.5162f, 11.3886f, 1.1416f, 11.4909f, 1.5882f, 11.1543f)
                lineTo(3.0001f, 10.0906f)
                lineTo(4.0001f, 20.7479f)
                curveTo(4.0001f, 21.8525f, 4.8955f, 22.7479f, 6.0001f, 22.7479f)
                horizontalLineTo(18.0f)
                curveTo(19.1046f, 22.7479f, 20.0f, 21.8525f, 20.0f, 20.7479f)
                lineTo(21.0f, 10.0907f)
                lineTo(22.4118f, 11.1543f)
                curveTo(22.8584f, 11.4909f, 23.4838f, 11.3886f, 23.8087f, 10.9259f)
                curveTo(24.1335f, 10.4631f, 24.0348f, 9.8152f, 23.5881f, 9.4787f)
                close()
            }
        }
        .build()
        return _homefilled!!
    }

private var _homefilled: ImageVector? = null
