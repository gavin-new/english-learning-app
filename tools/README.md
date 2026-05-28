# 词汇数据生成脚本

这个Python脚本可以帮你快速生成完整的词汇JSON数据文件。

## 使用方法

1. 安装Python 3.x
2. 运行脚本：
```bash
python generate_vocabulary.py
```

## 输出文件

脚本会在 `assets/vocabulary/` 目录下生成以下文件：
- grade4.json - 小学四年级词汇
- grade5.json - 小学五年级词汇
- grade6.json - 小学六年级词汇
- middle_school.json - 中考词汇
- high_school.json - 高考词汇
- cet4.json - 四级词汇
- cet6.json - 六级词汇

## 自定义词汇

你可以编辑 `vocabulary_lists.py` 文件来添加自定义词汇。

## 数据格式

每个词汇包含以下字段：
- word: 单词
- phonetic: 音标
- meaning: 释义
- example: 例句
- exampleTranslation: 例句翻译
- level: 难度等级(1-5)
- category: 分类
- grade: 年级代码
- prefix: 前缀(可选)
- suffix: 后缀(可选)
- root: 词根(可选)
- rootMeaning: 词根含义(可选)
