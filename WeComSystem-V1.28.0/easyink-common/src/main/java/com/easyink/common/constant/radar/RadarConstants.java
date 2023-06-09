package com.easyink.common.constant.radar;

import com.easyink.common.enums.radar.RadarChannelEnum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * ClassName： RadarConstants
 *
 * @author wx
 * @date 2022/7/18 17:30
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RadarConstants {


    /**
     * 参数校验异常
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ParamVerify {
        /**
         * 传输参数为空
         */
        public static final String PARAM_NULL = "传输参数为空";
    }

    /**
     * 提示客户，自定义抛出异常给前端使用
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PromptCus {
        /**
         * 未选择标签
         */
        public static final String NOT_USE_TAG = "未选择客户标签";
        /**
         * 渠道名称重复
         */
        public static final String RADAR_CHANNEL_REPEAT = "渠道名称重复";

        public static final String RADAR_SHORT_PARAM_ERROR = "生成雷达短链参数错误";

        public static final String RADAR_SHORT_ERROR = "雷达不存在";
    }

    /**
     * 新增/更新通知员工
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UpdateNoticeToUser {
        /**
         * 发送消息，企业雷达库已更新
         */
        public static final String CORP_RADAR_UPDATE = "企业雷达库已更新";

        /**
         * 发送消息，部门雷达库已更新
         */
        public static final String DEPARTMENT_RADAR_UPDATE = "部门雷达库已更新";

        // “雷达标题”
        public static String getUpdateMessage(String title) {
            return "“" + title + "”";
        }

        public static final String SEND_ALL = "@all";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RadarAnalyseCount {
        /**
         * 如果不传日期默认为7天的数据
         */
        public static final Integer DEFAULT_DAY = 7;

        /**
         * 数量为0
         */
        public static final Integer ZERO = 0;
    }

    /**
     * 客户点击记录
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RadarCustomerClickRecord {

        public static final String COMMON_MSG = "普通欢迎语";

        public static final String EMPTY_SCENARIO = "";
        /**
         * 替换渠道来源内容
         */
        public static final String REPlACE_WORD = "【替换内容】";

        public static String getRecordText(String customerName, String userName, String detail, int type, String channelName) {
            String msg = "客户“" + customerName + "”" + "点击了员工“" + userName + "”" + "在【" + RadarChannelEnum.getChannelByType(type) + REPlACE_WORD + "】发出的雷达链接";
            if (RadarChannelEnum.CUSTOMIZE.getTYPE().equals(type)) {
                return msg.replace(REPlACE_WORD, "-{" + channelName + "}");
            }
            if (StringUtils.isNotBlank(detail)
                    && !RadarChannelEnum.MOMENT.getTYPE().equals(type)
                    && !RadarChannelEnum.SIDE_BAR.getTYPE().equals(type)
                    && !RadarChannelEnum.WELCOME_MSG.getTYPE().equals(type)) {
                return msg.replace(REPlACE_WORD, "-{" + detail + "}");
            } else {
                return msg.replace(REPlACE_WORD, StringUtils.EMPTY);
            }
        }
    }

}
