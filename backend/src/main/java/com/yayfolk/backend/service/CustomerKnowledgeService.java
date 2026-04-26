package com.yayfolk.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CustomerKnowledgeService {

    private static final List<KnowledgeItem> KNOWLEDGE_BASE = new ArrayList<>();

    static {
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("注册", "注册账号", "怎么注册", "如何注册", "创建账号", "新账号"),
                "注册步骤：1. 打开YayFolk应用 → 2. 点击登录页的\"注册\"按钮 → 3. 填写账号、密码、邮箱 → 4. 输入邮箱验证码 → 5. 完成注册。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("登录", "怎么登录", "无法登录", "登录失败", "登不上去", "登录不了"),
                "登录方式：1. 账号密码登录 → 输入账号和密码即可；2. 验证码登录 → 输入账号，获取邮箱验证码后登录。如忘记密码，可点击\"忘记密码\"通过邮箱重置。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("忘记密码", "重置密码", "修改密码", "找回密码", "密码忘了"),
                "找回密码步骤：1. 在登录页点击\"忘记密码\" → 2. 输入注册邮箱 → 3. 获取验证码 → 4. 设置新密码。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("非遗", "非物质文化遗产", "浏览非遗", "看非遗", "非遗项目", "非遗文化", "传统文化"),
                "浏览非遗项目：1. 进入首页 → 点击\"非遗\"标签 → 2. 可按分类（传统音乐、传统舞蹈、传统技艺等）筛选 → 3. 点击任意项目查看详情，包括历史渊源、传承人信息等。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("活动", "参加活动", "预约活动", "报名活动", "活动预约", "活动报名", "怎么参加", "如何参加活动"),
                "参加活动步骤：1. 进入首页 → 点击\"活动\"标签 → 2. 浏览活动列表，可按地区、类型筛选 → 3. 点击感兴趣的活动 → 4. 点击\"立即预约\" → 5. 填写参与人信息 → 6. 确认支付（免费活动无需支付） → 7. 预约成功，可在\"个人中心-我的活动\"查看。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("发帖", "发布帖子", "写帖子", "发动态", "发发现", "怎么发帖", "如何发帖", "分享"),
                "发帖步骤：1. 进入\"发现\"页面 → 2. 点击右下角的\"+\"按钮 → 3. 输入帖子内容，可添加图片和标签 → 4. 点击\"发布\"即可。发帖后可在\"个人中心-我的帖子\"管理。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("评论", "写评论", "怎么评论", "发表评论"),
                "评论步骤：1. 打开任意帖子 → 2. 在底部输入框输入评论内容 → 3. 点击发送即可。您也可以回复他人的评论。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("收藏", "怎么收藏", "收藏帖子", "取消收藏"),
                "收藏步骤：1. 打开任意帖子 → 2. 点击帖子下方的收藏图标即可收藏 → 3. 再次点击可取消收藏。收藏的帖子可在\"个人中心-我的收藏\"查看。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("商家入驻", "申请商家", "成为商家", "商户入驻", "商家申请"),
                "商家入驻步骤：1. 进入\"个人中心\" → 2. 点击\"商家入驻\" → 3. 填写商家名称、联系方式、营业执照等信息 → 4. 提交申请 → 5. 等待管理员审核，审核通过后即可使用商家功能。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("个人中心", "个人信息", "修改资料", "编辑资料", "修改头像", "换头像", "个人设置"),
                "个人中心功能：1. 点击底部\"个人\"标签进入 → 2. 点击头像区域可编辑个人资料（昵称、头像、简介等） → 3. 可查看我的帖子、收藏、活动、关注等。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("关注", "怎么关注", "关注用户", "取消关注", "粉丝"),
                "关注功能：1. 进入其他用户的主页 → 2. 点击\"关注\"按钮即可关注 → 3. 再次点击可取消关注。关注列表和粉丝列表可在个人主页查看。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("AI路线", "智能路线", "非遗路线", "规划路线", "行程规划", "旅游路线", "路线推荐"),
                "AI路线规划：1. 进入首页 → 点击\"工具\"标签 → 2. 选择\"AI非遗路线规划\" → 3. 输入目的地、天数、偏好等 → 4. AI将为您生成个性化非遗旅行路线 → 5. 可保存路线供后续查看。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("订单", "我的订单", "查看订单", "订单详情", "支付"),
                "查看订单：1. 进入\"个人中心\" → 2. 点击\"我的活动\" → 3. 可查看所有预约记录和支付状态 → 4. 点击具体订单可查看详情、二维码等。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("退款", "取消预约", "取消订单", "退钱"),
                "取消预约/退款：1. 进入\"个人中心\" → \"我的活动\" → 2. 找到对应订单 → 3. 点击\"取消预约\" → 4. 如已支付，退款将按原渠道返回。注意：活动开始前可取消，具体退款规则以活动说明为准。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("签到", "核销", "活动签到", "二维码签到"),
                "活动签到：1. 进入\"个人中心\" → \"我的活动\" → 2. 找到对应订单 → 3. 出示签到二维码 → 4. 商家扫码完成签到。也可在活动详情页直接出示二维码。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("举报", "投诉", "举报帖子", "举报用户"),
                "举报功能：1. 打开要举报的帖子 → 2. 点击右上角\"...\"菜单 → 3. 选择\"举报\" → 4. 选择举报原因并提交。我们会尽快处理您的举报。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("翻译", "多语言", "英文", "翻译帖子", "翻译评论"),
                "翻译功能：1. 在帖子或评论中点击翻译图标 → 2. 系统将自动翻译为您的首选语言 → 3. 支持中英日韩等多种语言互译。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("通知", "消息通知", "评论通知", "收藏通知", "看不到通知"),
                "通知查看：1. 点击底部导航栏的通知图标 → 2. 可查看评论通知和收藏通知 → 3. 点击通知可跳转到对应帖子。"
        ));
        KNOWLEDGE_BASE.add(new KnowledgeItem(
                Arrays.asList("封禁", "被封", "账号封禁", "解封", "申诉"),
                "账号封禁申诉：1. 尝试登录时会提示账号已被封禁 → 2. 点击\"申请解封\" → 3. 填写申诉理由 → 4. 等待管理员审核。"
        ));
    }

    public String match(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        String input = userInput.trim().toLowerCase();
        for (KnowledgeItem item : KNOWLEDGE_BASE) {
            for (String keyword : item.keywords) {
                if (input.contains(keyword.toLowerCase())) {
                    return item.answer;
                }
            }
        }
        return null;
    }

    private static class KnowledgeItem {
        final List<String> keywords;
        final String answer;

        KnowledgeItem(List<String> keywords, String answer) {
            this.keywords = keywords;
            this.answer = answer;
        }
    }
}
