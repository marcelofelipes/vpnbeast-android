package com.b.android.openvpn60.constant;

/**
 * Created by b on 8/16/17.
 */

public enum AppConstants {

    USER_NAME("user_name"),
    USER_UUID("user_uuid"),
    TEMP_USER("temp_user"),
    USER_PASS("user_pass"),
    USER_CHOICE("user_choice"),
    SHARED_PREFS("shared_prefs"),
    MEMBER_NAME("member_name"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    EMAIL("email"),
    RESULT_PROFILE("result_profile"),
    SELECTED_PROFILE("selected_profile"),
    CMD_PING("ping -c 2 -w 2 "),
    CLASS_TAG_ACTIVITY(""),
    EXTRA_KEY("shortcut_profile_uuid"),
    EXTRA_NAME("shortcut_profile_ip"),
    EXTRA_HIDELOG("show_no_log_window"),
    CLEARLOG("clear_log_connect"),
    PROFILE_NAME("profile_name"),
    PROFILE_IP("profile_ip"),
    PROFILE_PORT("profile_port"),
    PROFILE_STATUS("profile_status"),
    CLOSE_ACTIVITY("CLOSE_ACTIVITY"),
    DISCONNECT_VPN("DISCONNECT_VPN"),
    RESULT_DESTROYED("RESULT_DESTROYED");

    private final String text;

    AppConstants(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}