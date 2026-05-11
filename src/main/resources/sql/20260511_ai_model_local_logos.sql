-- Use local frontend SVG assets for AI model logos.
-- Safe to run repeatedly.

UPDATE `ai_model`
SET `logo_url` = CASE `vendor_name`
    WHEN 'OpenAI' THEN '/images/logo/openai@logotyp.us.svg'
    WHEN 'Anthropic' THEN '/images/logo/anthropic-1.svg'
    WHEN 'Google' THEN '/images/logo/google-ai-1.svg'
    WHEN 'DeepSeek' THEN '/images/logo/deepseek-2.svg'
    WHEN 'Alibaba Cloud' THEN '/images/logo/alibaba-cloud-1.svg'
    WHEN 'Moonshot AI' THEN '/images/logo/moonshot-ai.svg'
    WHEN 'Zhipu AI' THEN '/images/logo/zhipu-ai.svg'
    WHEN 'Baidu' THEN '/images/logo/baidu-icon.svg'
    WHEN 'xAI' THEN '/images/logo/xai-2.svg'
    WHEN 'Meta' THEN '/images/logo/meta-3.svg'
    WHEN 'Mistral AI' THEN '/images/logo/mistral-2.svg'
    ELSE `logo_url`
END
WHERE `vendor_name` IN (
    'OpenAI',
    'Anthropic',
    'Google',
    'DeepSeek',
    'Alibaba Cloud',
    'Moonshot AI',
    'Zhipu AI',
    'Baidu',
    'xAI',
    'Meta',
    'Mistral AI'
);
