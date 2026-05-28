# 词汇数据生成工具

这个Python脚本用于生成完整的英语词汇JSON数据文件。

## 使用方法

```bash
python generate_vocabulary.py
```

## 输出文件

- grade3.json - 小学三年级词汇
- grade4.json - 小学四年级词汇
- grade5.json - 小学五年级词汇
- grade6.json - 小学六年级词汇
- middle_school.json - 中考词汇
- high_school.json - 高考词汇
- cet4.json - 四级词汇
- cet6.json - 六级词汇

## 数据格式

```json
[
  {
    "word": "example",
    "phonetic": "/ɪɡˈzɑːmpl/",
    "meaning": "n. 例子；榜样",
    "example": "This is an example.",
    "exampleTranslation": "这是一个例子。",
    "level": 2,
    "category": "中考词汇",
    "grade": 7,
    "prefix": null,
    "suffix": null,
    "root": "ample",
    "rootMeaning": "拿"
  }
]
```

## 年级代码

- 1-6: 小学1-6年级
- 7: 中考词汇
- 8: 高考词汇
- 9: 四级词汇
- 10: 六级词汇

## 难度等级

- 1: 基础词汇
- 2: 初级词汇
- 3: 中级词汇
- 4: 中高级词汇
- 5: 高级词汇
