// utils/coze.js - 豆包(Coze) AI Agent 流式调用工具

// 1. 基础配置（严格按你提供的示例，只是修正拼写和语法）
//const COZE_CONFIG = {
//  BOT_ID: '7584478614508027945',
//  API_KEY: 'pat_y6zA8YUs0dRZr5sqDNjomAUhIl94VGM0ebjp9SGQb5khHZgxahq1Lcl8rLA7LG5x',
//  API_URL: 'https://api.coze.cn/v3/chat',
//};

const COZE_CONFIG = {
  BOT_ID: '7587462701082411042',
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
    // 记录开始时间
    const startTime = Date.now();
    console.log(`[Coze API] 开始调用，时间: ${new Date().toISOString()}`);

    if (!decoder) {
      reject(new Error('当前环境不支持 TextDecoder，无法处理流式响应'));
      return;
    }

    const requestStartTime = Date.now();
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
        const requestDuration = Date.now() - requestStartTime;
        console.log(`[Coze API] 请求已发送，耗时: ${requestDuration}ms`);
      },
      fail: (error) => {
        const failDuration = Date.now() - startTime;
        console.error(`[Coze API] 请求失败，总耗时: ${failDuration}ms`, error);
        reject(error);
      },
    });

    if (!requestTask || typeof requestTask.onChunkReceived !== 'function') {
      reject(new Error('当前基础库不支持 onChunkReceived，无法使用流式响应'));
      return;
    }

    let buffer = '';
    let followUpQuestions = [];
    let fullContent = '';
    let firstChunkTime = null;
    let chunkCount = 0;

    requestTask.onChunkReceived((response) => {
      const chunkStartTime = Date.now();
      chunkCount++;
      
      // 记录第一个 chunk 的接收时间
      if (firstChunkTime === null) {
        firstChunkTime = Date.now();
        const timeToFirstChunk = firstChunkTime - requestStartTime;
        console.log(`[Coze API] 收到第一个数据块，耗时: ${timeToFirstChunk}ms`);
      }
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
                const totalDuration = Date.now() - startTime;
                const processingDuration = Date.now() - chunkStartTime;
                console.log(`[Coze API] 处理完成 - 总耗时: ${totalDuration}ms, 最后chunk处理: ${processingDuration}ms, 总chunk数: ${chunkCount}`);
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
            console.error(`[Coze API] JSON 解析失败，chunk处理耗时: ${Date.now() - chunkStartTime}ms`, e, dataStr);
          }
        }
        
        // 记录每个 chunk 的处理耗时（每10个chunk打印一次，避免日志过多）
        if (chunkCount % 10 === 0) {
          const chunkProcessDuration = Date.now() - chunkStartTime;
          console.log(`[Coze API] 已处理 ${chunkCount} 个chunk，当前chunk处理耗时: ${chunkProcessDuration}ms`);
        }
      } catch (error) {
        const errorDuration = Date.now() - startTime;
        console.error(`[Coze API] 处理流式数据失败，总耗时: ${errorDuration}ms`, error);
        reject(error);
      }
    });
  });
}

module.exports = {
  callCozeAPI,
};

