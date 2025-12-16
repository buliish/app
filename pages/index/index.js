// index.js
Page({
  data: {
    inputValue: '',
    sendIcon:
      'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><path fill="%23fff" d="M5.1 6.37 41.4 21.5a2 2 0 0 1 0 3.72L5.1 40.37a1.5 1.5 0 0 1-2.04-1.83l4.14-14.4-4.14-14.7A1.5 1.5 0 0 1 5.1 6.37zM10.7 24l-2.77 9.6L34.4 24 7.93 14.07 10.7 24z"/></svg>',
    messages: [
      {
        id: 'm-hello',
        sender: 'bot',
        text: '你好，请输入任何文本，我都会将它翻译成地道的英文。',
        suggestions: [],
      },
    ],
    scrollToView: 'msg-m-hello',
  },

  onInputChange(e) {
    this.setData({ inputValue: e.detail.value });
  },

  onSend() {
    const text = (this.data.inputValue || '').trim();
    if (!text) return;

    const userMsg = this.createMessage('user', text);
    const botMsg = this.createBotReply(text);

    const nextMessages = [...this.data.messages, userMsg, botMsg];
    this.setData({
      messages: nextMessages,
      inputValue: '',
      scrollToView: `msg-${botMsg.id}`,
    });
  },

  onSuggestionTap(e) {
    const question = e.currentTarget.dataset.question;
    this.setData({ inputValue: question }, () => this.onSend());
  },

  createMessage(sender, text, suggestions = []) {
    return {
      id: `${sender}-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      sender,
      text,
      suggestions,
    };
  },

  createBotReply(originalText) {
    const translated = this.mockTranslate(originalText);
    const followups = [
      '告诉我一些关于日期的小知识',
      '介绍一下闰年的判断规则',
      '今年的农历新年是几月几号',
    ];

    return this.createMessage('bot', translated, followups);
  },

  mockTranslate(text) {
    if (!text) return 'Here is your translation.';
    return `“${text}” 的英文可以这样说：${this.simpleTranslate(text)}`;
  },

  simpleTranslate(text) {
    // 演示用的占位逻辑，后续可接入真实 AI Agent
    if (/星期|周|礼拜/.test(text)) {
      return 'What day is it today?';
    }
    return `English: ${text}`;
  },
});
