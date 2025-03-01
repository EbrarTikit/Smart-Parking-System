from typing import List, Dict
from datetime import datetime, timedelta

class ConversationHistory:
    def __init__(self, max_history: int = 5):
        self.conversations: Dict[str, List[Dict]] = {}
        self.max_history = max_history
        self.expiry_time = timedelta(minutes=30)

    def add_message(self, session_id: str, role: str, content: str) -> None:
        if session_id not in self.conversations:
            self.conversations[session_id] = []
        
        self.conversations[session_id].append({
            'role': role,
            'content': content,
            'timestamp': datetime.now()
        })
        

        if len(self.conversations[session_id]) > self.max_history * 2: 
            self.conversations[session_id] = self.conversations[session_id][-self.max_history * 2:]

    def get_conversation(self, session_id: str) -> List[Dict]:
        return self.conversations.get(session_id, [])

    def clear_conversation(self, session_id: str) -> None:
        if session_id in self.conversations:
            del self.conversations[session_id]

    def cleanup_expired(self) -> None:
        current_time = datetime.now()
        for session_id in list(self.conversations.keys()):
            if self.conversations[session_id]:
                last_message_time = self.conversations[session_id][-1]['timestamp']
                if current_time - last_message_time > self.expiry_time:
                    self.clear_conversation(session_id) 