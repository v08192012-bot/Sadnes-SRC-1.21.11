package tech.sadnes.client.util.compat;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Set;

final class ViaProtocolInfo {
    private ViaProtocolInfo() {
    }

    static boolean isLegacyProtocol() {
        ProtocolVersion protocolVersion = getServerProtocol();
        return protocolVersion != null && protocolVersion.olderThan(ProtocolTranslator.NATIVE_VERSION);
    }

    static int getProtocolVersion() {
        ProtocolVersion protocolVersion = getServerProtocol();
        return protocolVersion == null ? -1 : protocolVersion.getVersion();
    }

    static String getVersionName() {
        ProtocolVersion protocolVersion = getServerProtocol();
        if (protocolVersion == null) {
            return null;
        }

        Set<String> strings = protocolVersion.getIncludedVersions();
        if (strings != null && !strings.isEmpty()) {
            String string = null;

            for (String string2 : strings) {
                if (string == null || ProtocolUtil.compareVersions(string2, string) > 0) {
                    string = string2;
                }
            }

            return string;
        } else {
            String string3 = protocolVersion.getName();
            int i = string3.lastIndexOf(45);
            return i > 0 && i + 1 < string3.length() && Character.isDigit(string3.charAt(i + 1)) ? string3.substring(i + 1) : string3;
        }
    }

    private static ProtocolVersion getServerProtocol() {
        try {
            UserConnection userConnection = ProtocolTranslator.getPlayNetworkUserConnection();
            ProtocolInfo protocolInfo = userConnection == null ? null : userConnection.getProtocolInfo();
            ProtocolVersion protocolVersion = protocolInfo == null ? ProtocolTranslator.getTargetVersion() : protocolInfo.serverProtocolVersion();
            return protocolVersion != null && protocolVersion != ProtocolTranslator.AUTO_DETECT_PROTOCOL && protocolVersion.isKnown() ? protocolVersion : null;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
