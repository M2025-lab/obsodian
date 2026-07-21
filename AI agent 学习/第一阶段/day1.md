## Day1：LLM 是什么（4小时）

> 目标：理解 ChatGPT 到底是什么。

---

### 第一部分（理论）

学习：

#### ① GPT是什么

搞懂：

- GPT是什么意思
    
- 为什么叫 Generative
    
- 为什么叫 Transformer
    

不用研究论文。

重点理解：

```
输入一句话

↓

拆成Token

↓

预测下一个Token

↓

一直预测

↓

组成一句话
```

---

#### ② LLM为什么能聊天

学习：

什么叫：

```
Next Token Prediction
```

理解：

为什么：

```
你好

↓

模型

↓

预测：

"！"

↓

预测：

"请问"

↓

预测：

"有什么"

↓

预测：

"可以"

...
```

所以：

ChatGPT没有真正思考。

只是不断预测。

---

#### ③ Token

今天重点：

研究Token。

需要知道：

- 一个汉字几个Token？
    
- 一个英文几个Token？
    
- 为什么：
    

```
你好
```

不是两个Token？

为什么：

```
SpringBoot
```

可能一个Token。
Tokenizer拆的，但是他怎么拆的。使用BPE算法，以及后续的byte-level BPE 等进行拆分，核心思想就是不断统计整个语料，Token 是"统计"出来的。如果“你好”很常见那就把他划分为一个token。


---

#### ④ Context Window

学习：

为什么：

```
聊天越来越慢
```

为什么：

```
聊天越来越贵
```

为什么：

```
模型会忘记前面内容
```

---

### 企业实践

企业为什么关心Token？

例如：

```
每天100万请求

×

1000Token

=

多少钱？
```

理解：

企业为什么：

Prompt要优化。

### 拆成多个 Prompt

不要：

一个 Prompt：

什么都干。

---

### 编码

今天：

不要SpringAI。

直接：

调用：

OpenAI API。

打印：

```
请求时间

Prompt

Response

Token

耗时
```

---

### 今天必须达到

能回答：

> 为什么LLM不是数据库？

> 为什么LLM会胡说？LLM 的目标从来不是"说真话"，而是"预测最可能的下一个 Token"。他只是预测下一次最有可能出现的token

> Token为什么收费？Transformer 每处理一个 Token，都要参与一次完整计算。每一个 Token 都意味着 GPU 要完成一次计算。Token 越多，占用 GPU 的时间越长，消耗的算力越多，因此成本也越高。
