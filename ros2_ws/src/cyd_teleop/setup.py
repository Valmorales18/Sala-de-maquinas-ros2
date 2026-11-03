from glob import glob

from setuptools import find_packages, setup

package_name = 'cyd_teleop'

setup(
    name=package_name,
    version='0.1.0',
    packages=find_packages(exclude=['test']),
    data_files=[
        ('share/ament_index/resource_index/packages', ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
        ('share/' + package_name + '/launch', glob('launch/*.launch.py')),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='Valentina Morales Rios',
    maintainer_email='vmoralesrios28@gmail.com',
    description='Teleoperación de turtlesim con la pantalla táctil del CYD por serial.',
    license='MIT',
    extras_require={'test': ['pytest']},
    entry_points={
        'console_scripts': [
            'driver_tactil = cyd_teleop.driver_tactil:main',
        ],
    },
)
