// utils/coze.js - 豆包(Coze) AI Agent 流式调用工具

// 1. 基础配置（严格按你提供的示例，只是修正拼写和语法）
const COZE_CONFIG = {
  BOT_ID: '7584478614508027945',
  API_KEY: 'pat_y6zA8YUs0dRZr5sqDNjomAUhIl94VGM0ebjp9SGQb5khHZgxahq1Lcl8rLA7LG5x',
  API_URL: 'https://api.coze.cn/v3/chat',
};

// 2. 初始化解码器（用于处理流式响应）
const decoder = typeof TextDecoder !== 'undefined' ? new TextDecoder('utf-8') : null;

/**
 * 3. 核心 API 调用方法
 * @param {string} message - 用户输入的消息
 * @param {(delta: string) => void} onUpdate - 流式增量回调，每次追加一小段内容
 * @returns {Promise<{content: string, followUpQuestions: string[]}>}
 */
function callCozeAPI(message, onUpdate) {
  return new Promise((resolve, reject) => {
    if (!decoder) {
      reject(new Error('当前环境不支持 TextDecoder，无法处理流式响应'));
      return;
    }

    const requestTask = wx.request({
      url: COZE_CONFIG.API_URL,
      method: 'POST',
      header: {
        Authorization: `Bearer ${COZE_CONFIG.API_KEY}`,
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      data: {
        bot_id: COZE_CONFIG.BOT_ID,
        user_id: 'user_id', // 你可以改成动态 userId
        stream: true,
        auto_save_history: true,
        additional_messages: [
          {
            role: 'user',
            type: 'question',
            content_type: 'text',
            content: message,
          },
        ],
      },
      enableChunked: true,
      success() {
        console.log('Coze 请求已发送');
      },
      fail: reject,
    });

    if (!requestTask || typeof requestTask.onChunkReceived !== 'function') {
      reject(new Error('当前基础库不支持 onChunkReceived，无法使用流式响应'));
      return;
    }

    let buffer = '';
    let followUpQuestions = [];
    let fullContent = '';

    requestTask.onChunkReceived((response) => {
      try {
        const uint8 = new Uint8Array(response.data);
        const chunk = decoder.decode(uint8);
        buffer += chunk;

        const events = buffer.split('\n\n');
        buffer = events.pop() || '';

        for (const event of events) {
          if (!event.trim()) continue;

          const lines = event.split('\n');
          if (lines.length < 2) continue;

          const eventType = lines[0].replace('event:', '').trim();
          const dataStr = lines[1].replace('data:', '').trim();
          if (!dataStr) continue;

          try {
            const data = JSON.parse(dataStr);

            switch (eventType) {
              case 'conversation.message.delta': {
                if (data.role === 'assistant' && data.type === 'answer' && data.content) {
                  fullContent += data.content;
                  if (typeof onUpdate === 'function') {
                    onUpdate(data.content);
                  }
                }
                break;
              }
              case 'conversation.message.completed': {
                if (data.role === 'assistant') {
                  if (data.type === 'answer' && data.content) {
                    if (!data.content.startsWith(fullContent)) {
                      const finalContent = data.content.slice(fullContent.length);
                      if (finalContent) {
                        fullContent += finalContent;
                        if (typeof onUpdate === 'function') {
                          onUpdate(finalContent);
                        }
                      }
                    }
                  } else if (data.type === 'follow_up' && data.content) {
                    followUpQuestions.push(data.content);
                  }
                }
                break;
              }
              case 'done': {
                resolve({
                  content: fullContent,
                  followUpQuestions,
                });
                break;
              }
              default:
                break;
            }
          } catch (e) {
            console.error('JSON 解析失败:', e, dataStr);
          }
        }
      } catch (error) {
        console.error('处理流式数据失败:', error);
        reject(error);
      }
    });
  });
}

module.exports = {
  callCozeAPI,
};

