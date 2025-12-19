// index.js
const { callCozeAPI } = require('../../utils/coze.js');

Page({
  data: {
    inputValue: '',
    sendIcon:
      'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><path fill="%23fff" d="M5.1 6.37 41.4 21.5a2 2 0 0 1 0 3.72L5.1 40.37a1.5 1.5 0 0 1-2.04-1.83l4.14-14.4-4.14-14.7A1.5 1.5 0 0 1 5.1 6.37zM10.7 24l-2.77 9.6L34.4 24 7.93 14.07 10.7 24z"/></svg>',
    messages: [
      {
        id: 'm-hello',
        sender: 'bot',
        text: '您好！我是能通过鱼病图片帮您诊断的智能助手，有鱼病问题随时发我图片，我来分析。',
        suggestions: [],
      },
    ],
    scrollToView: 'msg-m-hello',
    isLoading: false, // 是否正在加载 AI 回复
    userId: '', // 用户唯一标识
    pendingImages: [], // 待发送的图片列表
  },

  onLoad() {
    // 获取用户唯一标识（优先使用 openid，否则生成临时 ID）
    this.getUserId();
  },

  getUserId() {
    // 尝试从本地存储获取用户 ID
    let userId = wx.getStorageSync('coze_user_id');
    if (!userId) {
      // 如果没有，生成一个临时用户 ID
      userId = `wx_user_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
      wx.setStorageSync('coze_user_id', userId);
    }
    this.setData({ userId });
  },

  onInputChange(e) {
    this.setData({ inputValue: e.detail.value });
  },

  onMore() {
    wx.showActionSheet({
      itemList: ['拍照', '从相册选择', '从微信聊天记录中选择'],
      success: (res) => {
        const index = res.tapIndex;
        if (index === 0) {
          // 拍照
          wx.chooseImage({
            count: 1,
            sizeType: ['original', 'compressed'],
            sourceType: ['camera'],
            success: (result) => {
              const paths = result.tempFilePaths || [];
              this.addPendingImages(paths);
            },
          });
        } else if (index === 1) {
          // 相册
          wx.chooseImage({
            count: 9,
            sizeType: ['original', 'compressed'],
            sourceType: ['album'],
            success: (result) => {
              const paths = result.tempFilePaths || [];
              this.addPendingImages(paths);
            },
          });
        } else if (index === 2) {
          // 微信聊天记录
          wx.chooseMessageFile({
            count: 9,
            type: 'image',
            success: (result) => {
              const files = result.tempFiles || [];
              const paths = files.map((f) => f.path).filter(Boolean);
              this.addPendingImages(paths);
            },
          });
        }
      },
      fail(err) {
        console.log('more 操作取消或失败', err);
      },
    });
  },

  async onSend() {
    const text = (this.data.inputValue || '').trim();
    const hasText = !!text;
    const hasImages = this.data.pendingImages && this.data.pendingImages.length > 0;
    if (!hasText && !hasImages) return;

    if (this.data.isLoading) return;

    // 1. 先插入用户消息（文字 + 待发送图片）
    const userMsg = this.createMessage('user', text, [], this.data.pendingImages || []);
    const nextMessages = [...this.data.messages, userMsg];
    this.setData({
      messages: nextMessages,
      inputValue: '',
      isLoading: true,
      pendingImages: [], // 发送后清空待发送图片
    });

    // 2. 插入一个空的 bot 消息，用于流式填充
    const botMsgId = `bot-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    const botMsg = this.createMessage('bot', '', []);
    botMsg.id = botMsgId;

    const messagesWithBot = [...nextMessages, botMsg];
    this.setData({
      messages: messagesWithBot,
      scrollToView: `msg-${botMsgId}`,
    });

    try {
      // 3. 调用 Coze 流式 API
      const result = await callCozeAPI(text, (delta) => {
        this.appendToBotMessage(botMsgId, delta);
      });

      // 4. 完成后补齐内容和跟进问题
      this.finalizeBotMessage(botMsgId, result.content, result.followUpQuestions || []);
      this.setData({ isLoading: false });
    } catch (error) {
      console.error('调用 Coze API 失败:', error);
      const errorMsg = this.createMessage('bot', `抱歉，服务暂时不可用：${error.message || '未知错误'}`, []);
      const finalMessages = [...messagesWithBot, errorMsg];
      this.setData({
        messages: finalMessages,
        isLoading: false,
        scrollToView: `msg-${errorMsg.id}`,
      });
      wx.showToast({
        title: '发送失败，请重试',
        icon: 'none',
      });
    }
  },

  // 调用扣子平台 API（严格按照官方参考代码格式）
  async callCozeAPIStream(userMessage, botMsgId) {
    return new Promise((resolve, reject) => {
      const config = require('../../config.js');
      
      if (!config.COZE_API_KEY || !config.COZE_BOT_ID) {
        reject(new Error('请先配置 COZE_API_KEY 和 COZE_BOT_ID'));
        return;
      }

      // 严格按照官方参考代码格式（流式模式）
      wx.request({
        url: 'https://api.coze.cn/v3/chat',
        method: 'POST',
        header: {
          'Authorization': `Bearer ${config.COZE_API_KEY}`,
          'Content-Type': 'application/json',
        },
        enableChunked: true, // 启用分块传输以支持流式响应
        data: {
          bot_id: config.COZE_BOT_ID,
          user_id: this.data.userId || 'default_user',
          additional_messages: [
            {
              role: 'user',
              type: 'question',
              content_type: 'text',
              content: userMessage,
            },
          ],
          auto_save_history: true,
          stream: true,
        },
        success: (res) => {
          console.log('API 响应状态码:', res.statusCode);
          console.log('API 响应数据:', JSON.stringify(res.data));
          console.log('响应数据类型:', typeof res.data);
          console.log('响应数据长度:', res.data ? (typeof res.data === 'string' ? res.data.length : JSON.stringify(res.data).length) : 0);
          
          if (res.statusCode === 200) {
            // 如果流式响应为空，尝试使用非流式模式
            if (!res.data || res.data === '' || (typeof res.data === 'object' && Object.keys(res.data).length === 0)) {
              console.log('流式响应为空，尝试使用非流式模式重新请求');
              this.retryWithNonStream(userMessage, botMsgId, resolve, reject);
              return;
            }
            // 直接处理响应
            this.handleAPIResponse(res.data, botMsgId, resolve, reject);
          } else {
            reject(new Error(`API 请求失败: ${res.statusCode} - ${JSON.stringify(res.data)}`));
          }
        },
        fail: (err) => {
          console.error('API 请求失败:', err);
          reject(err);
        },
      });
    });
  },

  // 使用非流式模式重新请求（作为流式模式的备选方案）
  retryWithNonStream(userMessage, botMsgId, resolve, reject) {
    const config = require('../../config.js');
    
    wx.request({
      url: 'https://api.coze.cn/v3/chat',
      method: 'POST',
      header: {
        'Authorization': `Bearer ${config.COZE_API_KEY}`,
        'Content-Type': 'application/json',
      },
      data: {
        bot_id: config.COZE_BOT_ID,
        user_id: this.data.userId || 'default_user',
        additional_messages: [
          {
            role: 'user',
            type: 'question',
            content_type: 'text',
            content: userMessage,
          },
        ],
        auto_save_history: true,
        stream: false, // 使用非流式模式
      },
      success: (res) => {
        console.log('非流式模式响应状态码:', res.statusCode);
        console.log('非流式模式响应数据:', JSON.stringify(res.data));
        
        if (res.statusCode === 200) {
          this.handleAPIResponse(res.data, botMsgId, resolve, reject);
        } else {
          reject(new Error(`非流式模式请求失败: ${res.statusCode} - ${JSON.stringify(res.data)}`));
        }
      },
      fail: (err) => {
        console.error('非流式模式请求失败:', err);
        reject(err);
      },
    });
  },

  // 处理 API 响应（流式模式可能返回空字符串）
  handleAPIResponse(responseData, botMsgId, resolve, reject) {
    console.log('处理 API 响应:', JSON.stringify(responseData));
    console.log('响应数据类型:', typeof responseData);
    
    // 如果响应是空字符串，说明流式响应可能还在传输中
    if (responseData === '' || !responseData) {
      console.log('流式响应为空，可能是数据还在传输中');
      // 显示提示信息
      const tipMsg = this.createMessage('bot', '正在接收回复...', []);
      const messages = [...this.data.messages];
      const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
      if (botIndex !== -1) {
        messages[botIndex] = tipMsg;
        this.setData({ messages });
      }
      resolve(tipMsg);
      return;
    }
    
    let fullText = '';
    
    // 检查是否是异步任务
    if (responseData && typeof responseData === 'object' && responseData.data && responseData.data.status === 'in_progress') {
      console.log('检测到异步任务，状态: in_progress');
      // 直接显示提示信息，不等待
      const tipMsg = this.createMessage('bot', '消息已发送，AI 正在处理中，请稍候查看回复。', []);
      const messages = [...this.data.messages];
      const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
      if (botIndex !== -1) {
        messages[botIndex] = tipMsg;
        this.setData({ messages });
      }
      resolve(tipMsg);
      return;
    }
    
    // 尝试从响应中提取文本内容
    if (responseData && typeof responseData === 'object') {
      if (responseData.data) {
        const data = responseData.data;
        
        // 如果响应中包含消息内容
        if (data.messages && Array.isArray(data.messages) && data.messages.length > 0) {
          const assistantMsg = data.messages.find(
            (msg) => msg.role === 'assistant' || msg.role === 'bot' || msg.type === 'answer'
          );
          if (assistantMsg && assistantMsg.content) {
            fullText = assistantMsg.content;
          }
        } else if (data.content) {
          fullText = data.content;
        } else if (data.message && data.message.content) {
          fullText = data.message.content;
        }
      } else if (responseData.content) {
        fullText = responseData.content;
      } else if (responseData.messages && Array.isArray(responseData.messages)) {
        const assistantMsg = responseData.messages.find(
          (msg) => msg.role === 'assistant' || msg.role === 'bot'
        );
        if (assistantMsg && assistantMsg.content) {
          fullText = assistantMsg.content;
        }
      }
    } else if (typeof responseData === 'string' && responseData.trim()) {
      // 如果是字符串，尝试解析为 JSON 或直接使用
      try {
        const parsed = JSON.parse(responseData);
        if (parsed.content) {
          fullText = parsed.content;
        } else if (parsed.data && parsed.data.content) {
          fullText = parsed.data.content;
        }
      } catch (e) {
        // 如果不是 JSON，可能是纯文本
        fullText = responseData;
      }
    }
    
    // 如果有文本，使用打字机效果显示
    if (fullText) {
      console.log('提取到文本内容，长度:', fullText.length);
      this.typewriterEffect(botMsgId, fullText, () => {
        const botReply = this.createBotReplyFromAPI({ content: fullText });
        const messages = [...this.data.messages];
        const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
        if (botIndex !== -1) {
          messages[botIndex] = botReply;
          this.setData({ messages });
        }
        resolve(botReply);
      });
    } else {
      // 如果没有提取到文本，使用 createBotReplyFromAPI 处理
      console.log('未提取到文本，使用 createBotReplyFromAPI 处理');
      try {
        const botReply = this.createBotReplyFromAPI(responseData || {});
        const messages = [...this.data.messages];
        const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
        if (botIndex !== -1) {
          messages[botIndex] = botReply;
          this.setData({ messages });
        }
        resolve(botReply);
      } catch (e) {
        console.error('处理失败:', e);
        const errorMsg = this.createMessage('bot', `抱歉，无法解析 API 响应: ${e.message || '未知错误'}`, []);
        const messages = [...this.data.messages];
        const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
        if (botIndex !== -1) {
          messages[botIndex] = errorMsg;
          this.setData({ messages });
        }
        reject(new Error(`无法解析 API 响应: ${e.message || '未知错误'}`));
      }
    }
  },

  // 将选择的图片加入待发送列表
  addPendingImages(paths) {
    if (!paths || !paths.length) return;
    const current = this.data.pendingImages || [];
    const next = current.concat(paths);
    this.setData({
      pendingImages: next,
    });
  },

  // 追加流式内容到当前 bot 消息
  appendToBotMessage(botMsgId, delta) {
    if (!delta) return;
    const messages = [...this.data.messages];
    const idx = messages.findIndex((m) => m.id === botMsgId);
    if (idx === -1) return;
    const oldText = messages[idx].text || '';
    messages[idx].text = oldText + delta;
    this.setData({
      messages,
      scrollToView: `msg-${botMsgId}`,
    });
  },

  // 流式结束后，补齐最终内容和跟进问题
  finalizeBotMessage(botMsgId, fullText, followUpQuestions) {
    const messages = [...this.data.messages];
    const idx = messages.findIndex((m) => m.id === botMsgId);
    if (idx === -1) return;

    if (fullText) {
      messages[idx].text = fullText;
    }

    if (Array.isArray(followUpQuestions) && followUpQuestions.length) {
      messages[idx].suggestions = followUpQuestions.map((q) => ({
        value: q,
        label: this.truncateText(q, 18),
      }));
    }

    this.setData({
      messages,
      scrollToView: `msg-${botMsgId}`,
    });
  },



  // 打字机效果：逐字显示文本
  typewriterEffect(botMsgId, fullText, onComplete) {
    let currentIndex = 0;
    const speed = 30; // 每个字符的显示间隔（毫秒）
    
    const timer = setInterval(() => {
      if (currentIndex < fullText.length) {
        const displayText = fullText.substring(0, currentIndex + 1);
        this.updateBotMessage(botMsgId, displayText);
        currentIndex++;
      } else {
        clearInterval(timer);
        if (onComplete) {
          onComplete();
        }
      }
    }, speed);
  },

  // 更新 bot 消息内容（流式更新）
  updateBotMessage(botMsgId, text) {
    const messages = [...this.data.messages];
    const botIndex = messages.findIndex((msg) => msg.id === botMsgId);
    
    if (botIndex !== -1) {
      messages[botIndex].text = text;
      this.setData({
        messages,
        scrollToView: `msg-${botMsgId}`,
      });
    }
  },

  onSuggestionTap(e) {
    const question = e.currentTarget.dataset.question;
    this.setData({ inputValue: question }, () => this.onSend());
  },

  createMessage(sender, text, suggestions = [], images = []) {
    return {
      id: `${sender}-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      sender,
      text,
      suggestions,
      images,
    };
  },

  // 从 API 响应创建 bot 回复消息
  createBotReplyFromAPI(apiResponse) {
    console.log('createBotReplyFromAPI 收到的响应:', JSON.stringify(apiResponse).substring(0, 1000));
    
    // 解析 API 响应，提取 bot 的回复内容
    // 根据扣子平台 API 文档，响应结构可能包含 messages 数组
    let botText = '';
    let suggestions = [];

    // 尝试多种可能的响应格式
    if (apiResponse && apiResponse.messages && Array.isArray(apiResponse.messages) && apiResponse.messages.length > 0) {
      // 格式1: { messages: [{ role: 'assistant', content: '...' }] }
      const assistantMsg = apiResponse.messages.find(
        (msg) => msg.role === 'assistant' || msg.role === 'bot' || msg.type === 'answer'
      );
      if (assistantMsg) {
        botText = assistantMsg.content || assistantMsg.text || '';
        console.log('从 messages 数组中提取到文本，长度:', botText.length);
      }
    } else if (apiResponse && apiResponse.content) {
      // 格式2: { content: '...' }
      botText = apiResponse.content;
      console.log('从 content 字段提取到文本，长度:', botText.length);
    } else if (apiResponse && apiResponse.data) {
      // 格式3: { data: { messages: [...] } } 或 { data: { content: '...' } }
      const data = apiResponse.data;
      if (data.messages && Array.isArray(data.messages) && data.messages.length > 0) {
        const assistantMsg = data.messages.find(
          (msg) => msg.role === 'assistant' || msg.role === 'bot' || msg.type === 'answer'
        );
        if (assistantMsg) {
          botText = assistantMsg.content || assistantMsg.text || '';
          console.log('从 data.messages 中提取到文本，长度:', botText.length);
        }
      } else if (data.content) {
        botText = data.content;
        console.log('从 data.content 提取到文本，长度:', botText.length);
      }
    } else if (apiResponse && apiResponse.text) {
      // 格式4: { text: '...' }
      botText = apiResponse.text;
      console.log('从 text 字段提取到文本，长度:', botText.length);
    }

    // 如果没有找到回复内容，使用默认提示
    if (!botText) {
      console.warn('未能从响应中提取文本，完整响应:', JSON.stringify(apiResponse));
      botText = '抱歉，我没有收到有效的回复。请检查 API 响应格式。';
    }

    // 如果 API 响应中包含建议问题，可以在这里解析
    // 目前先使用空数组，后续可以根据实际 API 响应结构调整
    if (apiResponse && apiResponse.suggestions) {
      suggestions = apiResponse.suggestions.map((q) => ({
        value: q,
        label: this.truncateText(q, 18),
      }));
    }

    return this.createMessage('bot', botText, suggestions);
  },

  // 将额外问题的展示文案限制在固定长度，超出部分用 ... 省略
  truncateText(text, maxLen) {
    if (!text) return '';
    const str = String(text);
    if (str.length <= maxLen) return str;
    return `${str.slice(0, maxLen)}...`;
  },
});
