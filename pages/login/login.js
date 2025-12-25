import { userLogin } from '../../api/api' // 引入登录接口

Page({
  /**
   * 页面初始数据
   */
  data: {
    showPassword: false,  // 密码是否显示明文
    phone: '',            // 手机号
    password: ''          // 密码
  },

  // 切换密码显示/隐藏状态
  togglePasswordStatus() {
    this.setData({
      showPassword: !this.data.showPassword
    });
  },

  // 核心：登录逻辑
  async login() {
    const { phone, password } = this.data;
    console.log('登录参数 ==> ', { phone, password });

    // 1. 空值校验（补充：避免空值提交）
    if (!phone) {
      return wx.showToast({
        title: '请输入手机号',
        icon: 'none',
        mask: true
      });
    }
    if (!password) {
      return wx.showToast({
        title: '请输入密码',
        icon: 'none',
        mask: true
      });
    }

    // 2. 格式校验
    let phoneReg = /^1[3-9]\d{9}$/;
    if (!phoneReg.test(phone)) {
      wx.showToast({
        title: '手机号格式错误',
        icon: 'none',
        mask: true
      })
      return;
    }

    // 密码校验（和注册页规则一致）
    let passwordReg = /^[a-zA-Z][a-zA-Z0-9]{5,15}$/;
    if (!passwordReg.test(password)) {
      wx.showToast({
        title: '密码需以字母开头，6-16位数字/字母',
        icon: 'none',
        mask: true
      })
      return;
    }

    try {
      // 3. 显示加载状态（提升用户体验）
      wx.showLoading({
        title: '登录中...',
        mask: true // 防止穿透点击
      });

      // 4. 发起登录请求（修正笔误：passwod → password）
      let params = {
        phone: phone,
        password: password // 原代码笔误passwod，已修正
      };
      let data = await userLogin(params);
      console.log('登录接口返回 ==> ', data);

      // 5. 关闭加载
      wx.hideLoading();

      // 6. 处理接口返回结果
      wx.showToast({
        title: data.data?.msg || '登录成功', // 兼容返回值异常
        icon: 'none',
        mask: true // 修正笔误：msk → mask
      });

      // 7. 登录成功逻辑（code=200）
      if (data.data?.code === 200) {
        // 保存token到本地存储
        wx.setStorageSync('token34', data.data.token);
        console.log('token已保存：', data.data.token);
        
        // 保存用户信息
        const userInfo = {
          phone: phone,
          nickname: data.data.result?.nickname || phone,
          token: data.data.token
        };
        wx.setStorageSync('userInfo', userInfo);
        const app = getApp();
        app.globalData.userInfo = userInfo;
        
        // 重新初始化用户数据（购物车、收藏等）
        app.initCart();
        app.initFavorites();
        
        // 延迟跳转首页
        setTimeout(() => {
          wx.switchTab({
            url: '../home/home' // 确保home页在app.json的tabBar中注册
          });
        }, 1500);
      }
    } catch (error) {
      // 8. 异常捕获（网络错误/接口报错）
      wx.hideLoading();
      console.error('登录请求失败 ==> ', error);
      wx.showToast({
        title: '登录失败，请稍后重试',
        icon: 'none',
        mask: true
      });
    }
  },

  // 跳转到找回密码页面
  toFindPwd() {
    wx.showToast({
      title: '找回密码功能开发中',
      icon: 'none',
      mask: true
    });
  },

  // 跳转到注册页面
  toRegister() {
    wx.navigateTo({
      url: '../register/register' // 确保注册页路径正确
    });
    console.log('点击去注册，跳转路径：../register/register');
  }
})