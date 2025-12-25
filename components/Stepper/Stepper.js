// components/Stepper/Stepper.js
Component({
  properties: {
    value: {
      type: Number,
      value: 1
    },
    min: {
      type: Number,
      value: 1
    },
    max: {
      type: Number,
      value: 999
    }
  },

  methods: {
    onReduce() {
      if (this.data.value > this.data.min) {
        const newValue = this.data.value - 1
        this.setData({
          value: newValue
        })
        this.triggerEvent('change', { value: newValue })
      }
    },

    onAdd() {
      if (this.data.value < this.data.max) {
        const newValue = this.data.value + 1
        this.setData({
          value: newValue
        })
        this.triggerEvent('change', { value: newValue })
      }
    }
  }
})

