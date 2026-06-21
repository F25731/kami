"""
多项目卡密授权中心 - Python SDK
Version: 1.0.0
"""

import hashlib
import time
import random
import string
import requests
from typing import Dict, Optional, List


class KamiClient:
    """
    卡密授权中心客户端

    使用示例:
        client = KamiClient(
            base_url="https://api.example.com",
            project_token="abc123X",
            api_key="your_api_key",
            api_secret="your_api_secret"
        )

        # 验证卡密
        result = client.verify_card("ABCD-EFGH-IJKL")

        # 消费卡密
        result = client.consume_card("ABCD-EFGH-IJKL", biz_id="order_001", amount=1)
    """

    def __init__(self, base_url: str, project_token: str, api_key: str, api_secret: str, timeout: int = 30):
        """
        初始化客户端

        :param base_url: API 基础地址，例如 https://api.example.com
        :param project_token: 7位项目访问码
        :param api_key: API Key
        :param api_secret: API Secret
        :param timeout: 请求超时时间（秒）
        """
        self.base_url = base_url.rstrip('/')
        self.project_token = project_token
        self.api_key = api_key
        self.api_secret = api_secret
        self.timeout = timeout
        self.endpoint_prefix = f"/api/p/{project_token}"

    def _generate_signature(self, timestamp: int, nonce: str, body: str) -> str:
        """生成 HMAC-SHA256 签名"""
        data = f"{self.api_key}{timestamp}{nonce}{body}{self.api_secret}"
        return hashlib.sha256(data.encode('utf-8')).hexdigest()

    def _generate_nonce(self, length: int = 16) -> str:
        """生成随机 nonce"""
        return ''.join(random.choices(string.ascii_letters + string.digits, k=length))

    def _request(self, endpoint: str, payload: Optional[Dict] = None) -> Dict:
        """
        发送 API 请求

        :param endpoint: 端点路径，例如 /cards/verify
        :param payload: 请求体（字典）
        :return: 响应 JSON
        """
        timestamp = int(time.time())
        nonce = self._generate_nonce()
        body = ""

        if payload:
            import json
            body = json.dumps(payload, separators=(',', ':'), ensure_ascii=False)

        signature = self._generate_signature(timestamp, nonce, body)

        headers = {
            "Content-Type": "application/json",
            "X-API-KEY": self.api_key,
            "X-TIMESTAMP": str(timestamp),
            "X-NONCE": nonce,
            "X-SIGN": signature
        }

        url = f"{self.base_url}{self.endpoint_prefix}{endpoint}"

        try:
            response = requests.post(url, headers=headers, json=payload, timeout=self.timeout)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            return {
                "success": False,
                "code": "REQUEST_ERROR",
                "message": str(e)
            }

    # ========== 卡密相关接口 ==========

    def generate_cards(self, quantity: int, card_type: str = "count",
                       count_value: int = 0, duration_days: int = 0,
                       is_permanent: bool = False, order_no: Optional[str] = None,
                       package_id: Optional[int] = None, remark: Optional[str] = None) -> Dict:
        """
        生成卡密

        :param quantity: 生成数量
        :param card_type: 卡密类型 (count/time/hybrid/permanent)
        :param count_value: 次数值
        :param duration_days: 有效天数
        :param is_permanent: 是否永久
        :param order_no: 订单号（幂等）
        :param package_id: 套餐ID
        :param remark: 备注
        :return: 生成结果
        """
        payload = {
            "quantity": quantity,
            "card_type": card_type,
            "count_value": count_value,
            "duration_days": duration_days,
            "is_permanent": is_permanent
        }
        if order_no: payload["order_no"] = order_no
        if package_id: payload["package_id"] = package_id
        if remark: payload["remark"] = remark

        return self._request("/cards/generate", payload)

    def verify_card(self, card_key: str) -> Dict:
        """
        验证卡密

        :param card_key: 卡密
        :return: 验证结果
        """
        return self._request("/cards/verify", {"card_key": card_key})

    def get_card_status(self, card_key: str) -> Dict:
        """
        查询卡密状态

        :param card_key: 卡密
        :return: 状态信息
        """
        return self._request("/cards/status", {"card_key": card_key})

    def consume_card(self, card_key: str, biz_id: str, amount: int = 1,
                     device_id: Optional[str] = None) -> Dict:
        """
        消费卡密

        :param card_key: 卡密
        :param biz_id: 业务ID（幂等）
        :param amount: 消费次数
        :param device_id: 设备ID
        :return: 消费结果
        """
        payload = {
            "card_key": card_key,
            "biz_id": biz_id,
            "amount": amount
        }
        if device_id: payload["device_id"] = device_id

        return self._request("/cards/consume", payload)

    def redeem_card(self, card_key: str, user_id: str) -> Dict:
        """
        兑换卡密到用户权益

        :param card_key: 卡密
        :param user_id: 用户ID
        :return: 兑换结果
        """
        return self._request("/cards/redeem", {
            "card_key": card_key,
            "user_id": user_id
        })

    def unbind_device(self, card_key: str, reason: Optional[str] = None) -> Dict:
        """
        解绑设备

        :param card_key: 卡密
        :param reason: 解绑原因
        :return: 解绑结果
        """
        payload = {"card_key": card_key}
        if reason: payload["reason"] = reason

        return self._request("/cards/unbind-device", payload)

    def refund_consume(self, card_key: str, biz_id: str) -> Dict:
        """
        退还消费

        :param card_key: 卡密
        :param biz_id: 原消费的业务ID
        :return: 退还结果
        """
        return self._request("/cards/refund-consume", {
            "card_key": card_key,
            "biz_id": biz_id
        })

    # ========== 用户权益相关接口 ==========

    def get_entitlement_status(self, user_id: str, entitlement_type: str) -> Dict:
        """
        查询用户权益状态

        :param user_id: 用户ID
        :param entitlement_type: 权益类型
        :return: 权益状态
        """
        return self._request("/entitlements/status", {
            "user_id": user_id,
            "entitlement_type": entitlement_type
        })

    def consume_entitlement(self, user_id: str, entitlement_type: str,
                           biz_id: str, amount: int = 1) -> Dict:
        """
        消费用户权益

        :param user_id: 用户ID
        :param entitlement_type: 权益类型
        :param biz_id: 业务ID（幂等）
        :param amount: 消费次数
        :return: 消费结果
        """
        return self._request("/entitlements/consume", {
            "user_id": user_id,
            "entitlement_type": entitlement_type,
            "biz_id": biz_id,
            "amount": amount
        })

    def refund_entitlement(self, user_id: str, entitlement_type: str,
                          biz_id: str, amount: int = 1) -> Dict:
        """
        退还权益消费

        :param user_id: 用户ID
        :param entitlement_type: 权益类型
        :param biz_id: 原消费的业务ID
        :param amount: 退还次数
        :return: 退还结果
        """
        return self._request("/entitlements/refund-consume", {
            "user_id": user_id,
            "entitlement_type": entitlement_type,
            "biz_id": biz_id,
            "amount": amount
        })


# ========== 使用示例 ==========

if __name__ == "__main__":
    # 初始化客户端
    client = KamiClient(
        base_url="https://api.example.com",
        project_token="abc123X",
        api_key="your_api_key_here",
        api_secret="your_api_secret_here"
    )

    # 示例1：生成卡密
    print("=== 生成卡密 ===")
    result = client.generate_cards(
        quantity=5,
        card_type="count",
        count_value=100,
        order_no="ORDER20260621001"
    )
    print(result)

    # 示例2：验证卡密
    if result.get("success") and result.get("cards"):
        card_key = result["cards"][0]["card_key"]
        print(f"\n=== 验证卡密: {card_key} ===")
        result = client.verify_card(card_key)
        print(result)

        # 示例3：消费卡密
        print(f"\n=== 消费卡密 ===")
        result = client.consume_card(
            card_key=card_key,
            biz_id="user123_20260621_001",
            amount=1
        )
        print(result)

        # 示例4：查询状态
        print(f"\n=== 查询状态 ===")
        result = client.get_card_status(card_key)
        print(result)
