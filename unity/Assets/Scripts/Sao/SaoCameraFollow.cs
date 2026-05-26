using UnityEngine;

namespace Sao
{
    public class SaoCameraFollow : MonoBehaviour
    {
        public Transform target;
        public float smooth = 10f;

        private void LateUpdate()
        {
            if (target == null) return;
            var desired = new Vector3(target.position.x, target.position.y, transform.position.z);
            transform.position = Vector3.Lerp(transform.position, desired, 1f - Mathf.Exp(-smooth * Time.deltaTime));
        }
    }
}
