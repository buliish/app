import { userRegister } from '../../api/api'
Page({
  /**
   * 页面的初始数据
   */
  data: {
    showPassword: false,  // 密码是否显示明文
    phone: '',            // 手机号
    password: '',         // 密码
    nickname: ''          // 昵称
  },

  // 核心：切换密码显示/隐藏状态（和WXML bind:tap="togglePasswordStatus" 匹配）
  togglePasswordStatus() {
    console.log('密码显示状态切换');
    this.setData({
      showPassword: !this.data.showPassword
    });
  },

  // 核心：注册按钮点击逻辑（保留此方法，和WXML bind:tap="register" 匹配）
  async register() {
    console.log('this.data.phone ==> ', this.data.phone);
    console.log('this.data.password ==> ', this.data.password);
    console.log('this.data.nickname ==> ', this.data.nickname);

    // 校验注册表单
    // 手机号正则：11位有效手机号
    let phoneReg = /^1[3-9]\d{9}$/;
    if (!phoneReg.test(this.data.phone)) {
      wx.showToast({
        title: '手机号格式错误',
        icon: 'none',
        mask: true
      })
      return;
    }

    // 密码正则：字母开头，6-16位数字/字母
    let passwordReg = /^[a-zA-Z][a-zA-Z0-9]{5,15}$/;
    if (!passwordReg.test(this.data.password)) {
      wx.showToast({
        title: '密码需以字母开头，6-16位数字/字母',
        icon: 'none',
        mask: true
      })
      return;
    }

    // 昵称正则：中英文组合，1-16位
    let nicknameReg = /^[a-zA-Z\u4e00-\u9fa5]{1,16}$/;
    if (!nicknameReg.test(this.data.nickname)) {
      wx.showToast({
        title: '昵称需为中英文组合，1-16位',
        icon: 'none',
        mask: true
      })
      return;
    }

    //发起注册请求
    let params = {
      phone:this.data.phone,
      password: this.data.password,
      nickname: this.data.nickname 
    };
    let data = await userRegister(params);
    console.log('注册data ==> ',data);

    wx.showToast({
      title: data.data.msg,
      icon: 'none',
      msk: true
    })

    if (data.data.code == 100){
      setTimeout(() =>{
        this.goLogin()
      },1500)
    }
  },

  // 跳转到登录页面（备用，如需保留则留，不需要可删除）
  goLogin() {
    wx.navigateTo({ url: '../login/login' });
  }
})